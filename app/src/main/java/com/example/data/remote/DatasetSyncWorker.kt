package com.example.data.remote

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.example.data.local.AppDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class DatasetSyncWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = lock.withLock { sync() }
    private suspend fun sync(): Result {
        if (FirebaseApp.getApps(applicationContext).isEmpty()) return Result.success()
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return Result.success()
        val db = AppDatabase.getDatabase(applicationContext, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        val functions = FirebaseFunctions.getInstance()
        var failed = false
        try {
            for (operation in db.cloudDao().pending(uid)) {
                if (auth.currentUser?.uid != uid) return Result.success()
                try {
                    db.cloudDao().put(operation.copy(state = "UPLOADING", error = ""))
                    val data = JSONObject(operation.payload)
                    val input = data.keys().asSequence().associateWith { key -> data.get(key).takeUnless { it == JSONObject.NULL } }.toMutableMap()
                    input.putIfAbsent("dialectId", "Other")
                    input.putIfAbsent("regionId", "Other")
                    // Idempotent backend lookup prevents re-upload after an acknowledged metadata write.
                    val mediaKey = when(operation.collection) { "speech" -> "audioFilePath"; "images" -> "localUri"; else -> null }
                    if (mediaKey != null) {
                        val source = input.remove(mediaKey)?.toString().orEmpty()
                        val path = "${operation.collection}/$uid/${operation.recordId}"
                        input["mediaPath"] = path
                        val ref = FirebaseStorage.getInstance().reference.child(path)
                        val remoteExists = runCatching { ref.metadata.await() }.isSuccess
                        if (!remoteExists) {
                            val file = File(source)
                            require(file.isFile && file.length() > 0) { "Media file missing; select or record it again." }
                            val mime = if (operation.collection == "speech") "audio/mp4" else android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: error("Unknown image type")
                            ref.putFile(Uri.fromFile(file), StorageMetadata.Builder().setContentType(mime).build()).await()
                        }
                    }
                    functions.getHttpsCallable("submitDataset").call(mapOf("collection" to operation.collection, "recordId" to operation.recordId, "record" to input, "consent" to true, "consentVersion" to "1.0")).await()
                    db.cloudDao().put(operation.copy(state = "SYNCED", error = "", updatedAt = System.currentTimeMillis()))
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { failed = true; db.cloudDao().put(operation.copy(state = "FAILED", error = e.message ?: "Upload failed")) }
            }
            for (collection in DatasetCodec.collections) {
                var cursor: String? = null
                do {
                    if (auth.currentUser?.uid != uid) return Result.success()
                    val response = functions.getHttpsCallable("listDataset").call(mapOf("collection" to collection, "cursor" to cursor)).await().data as Map<*, *>
                    for (value in response["records"] as? List<*> ?: emptyList<Any>()) {
                        @Suppress("UNCHECKED_CAST") val record = value as Map<String, Any?>
                        val pending = db.cloudDao().get("$collection/${record["id"]}")
                        if (auth.currentUser?.uid != uid) return Result.success()
                        if (pending == null || pending.state == "SYNCED" || pending.state == "WITHDRAWN") DatasetCodec.save(db, collection, record)
                    }
                    cursor = response["cursor"] as? String
                } while (cursor != null)
            }
            if (auth.currentUser?.uid == uid) syncGovernance(db, functions, uid)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { return Result.retry() }
        return if (failed) Result.retry() else Result.success()
    }
    private suspend fun syncGovernance(db: AppDatabase, functions: FirebaseFunctions, uid: String) {
        val role = (functions.getHttpsCallable("getMyRbac").call().await().data as Map<*, *>)["role"].toString()
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val names = mutableListOf<String>()
        if(role in listOf("ADMIN","SUPER_ADMIN")) names.add("users")
        if(role in listOf("EXPERT","DATA_STEWARD","AUDITOR","ADMIN","SUPER_ADMIN")) names.add("auditLogs")
        if(role in listOf("EXPERT","DATA_STEWARD","RESEARCHER","ADMIN","SUPER_ADMIN")) names.add("dataset_versions")
        if(role in listOf("VALIDATOR","EXPERT","DATA_STEWARD","AUDITOR","ADMIN","SUPER_ADMIN")) names.add("validation_reviews")
        for(name in names) {
            var cursor: com.google.firebase.firestore.DocumentSnapshot? = null
            do {
                var query = firestore.collection(name).orderBy(com.google.firebase.firestore.FieldPath.documentId()).limit(100)
                if(cursor != null) query = query.startAfter(cursor)
                val page = query.get().await()
                if(FirebaseAuth.getInstance().currentUser?.uid != uid) return
                for(doc in page.documents) {
                    val data=doc.data.orEmpty()
                    fun str(key:String)=data[key]?.toString().orEmpty()
                    fun number(key:String)=(data[key] as? Number)?.toLong() ?: 0L
                    when(name) {
                        "users" -> db.userDao().insert(com.example.data.model.User(id=doc.id,email=str("email"),displayName=str("displayName"),username=str("username"),region=str("region"),role=runCatching { com.example.data.model.UserRole.valueOf(str("role")) }.getOrDefault(com.example.data.model.UserRole.CONTRIBUTOR)))
                        "auditLogs" -> db.metadataDao().insertAuditLog(com.example.data.model.AuditLog(id=doc.id,actorId=str("actorUid"),actorName=str("actorUid"),action=str("action"),entityType=str("collection"),entityId=str("recordId"),details=str("notes").ifEmpty { str("reason") },createdAt=number("createdAt")))
                        "dataset_versions" -> db.metadataDao().insertDatasetVersion(com.example.data.model.DatasetVersion(id=doc.id,versionNumber=str("versionNumber"),releaseName=str("releaseName"),description=str("description"),recordCount=number("recordCount").toInt(),speechHours=0.0,status=str("status"),createdBy=str("createdBy"),createdAt=number("createdAt")))
                        "validation_reviews" -> db.validationDao().insertReview(com.example.data.model.ValidationReview(id=doc.id,recordType=str("collection"),recordId=str("recordId"),validatorId=str("validatorId"),validatorName=str("validatorId"),decision=str("decision"),comments=str("comments"),confidenceScore=number("confidenceScore").toInt(),createdAt=number("createdAt")))
                    }
                }
                cursor = if(page.size()==100) page.documents.last() else null
            } while(cursor != null)
        }
    }
    companion object {
        val lock = Mutex()
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("dataset-periodic-sync", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DatasetSyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build())
            syncNow(context)
        }
        fun syncNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("dataset-sync", ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<DatasetSyncWorker>().setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
        }
    }
}
