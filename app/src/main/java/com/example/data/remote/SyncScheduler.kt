package com.example.data.remote

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.model.SyncOperation
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Adds local changes to a durable outbox and asks WorkManager to drain it when online. */
class SyncScheduler(
    private val context: Context,
    private val database: AppDatabase,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun enqueue(recordType: String, recordId: String, operationType: String = "CREATE") {
        val user = auth.currentUser ?: throw IllegalStateException("Sign in before queueing cloud sync.")
        require(user.isEmailVerified) { "Verify your email before queueing cloud sync." }
        val normalizedType = recordType.trim().uppercase()
        val normalizedOperation = operationType.trim().uppercase()
        require(normalizedOperation == "CREATE") { "Only CREATE operations are supported by the immutable cloud dataset." }
        val key = "${user.uid}:$normalizedType:$recordId:$normalizedOperation"
        database.syncOperationDao().insert(
            SyncOperation(
                id = UUID.randomUUID().toString(),
                idempotencyKey = key,
                recordType = normalizedType,
                recordId = recordId,
                operationType = normalizedOperation,
                ownerFirebaseUid = user.uid
            )
        )
        schedule()
    }

    fun schedule() {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val WORK_NAME = "khowar-cloud-sync"
    }
}
