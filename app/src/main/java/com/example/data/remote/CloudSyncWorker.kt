package com.example.data.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.math.min

/** Drains the Room outbox only on a connected, verified Firebase identity. */
class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val database = AppDatabase.getDatabase(
        appContext,
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )
    private val firebase = FirebaseSyncService()

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure()
        if (!user.isEmailVerified) return Result.failure()

        val queue = database.syncOperationDao()
        queue.recoverStale(System.currentTimeMillis() - STALE_UPLOAD_MS, System.currentTimeMillis())

        repeat(MAX_BATCHES) {
            val operations = queue.getDue(System.currentTimeMillis(), BATCH_SIZE)
            if (operations.isEmpty()) return Result.success()

            for (operation in operations) {
                if (operation.ownerFirebaseUid != user.uid) {
                    queue.markFailed(
                        operation.id,
                        "Authenticated Firebase UID does not own this queued operation.",
                        System.currentTimeMillis() + SECURITY_RETRY_MS,
                        System.currentTimeMillis()
                    )
                    continue
                }
                if (queue.markUploading(operation.id, System.currentTimeMillis()) == 0) continue

                try {
                    when (operation.operationType.uppercase()) {
                        "CREATE", "UPDATE" -> syncRecord(operation.recordType, operation.recordId)
                        else -> throw IllegalArgumentException("Unsupported sync operation: ${operation.operationType}")
                    }
                    queue.markCompleted(operation.id, System.currentTimeMillis())
                } catch (error: Throwable) {
                    val attempt = operation.attempts + 1
                    val delay = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * (1L shl min(attempt, 6)))
                    queue.markFailed(
                        operation.id,
                        error.message ?: error::class.java.simpleName,
                        System.currentTimeMillis() + delay,
                        System.currentTimeMillis()
                    )
                }
            }
        }
        return Result.success()
    }

    private suspend fun syncRecord(recordType: String, recordId: String) {
        when (recordType.uppercase()) {
            "LEXICON" -> database.lexiconDao().getById(recordId)?.let { firebase.syncLexicon(it) }
                ?: throw IllegalStateException("Local lexicon record not found: $recordId")
            "SENTENCE" -> database.sentenceDao().getById(recordId)?.let { firebase.syncSentence(it) }
                ?: throw IllegalStateException("Local sentence record not found: $recordId")
            "SPEECH" -> database.speechDao().getById(recordId)?.let { firebase.syncSpeech(it) }
                ?: throw IllegalStateException("Local speech record not found: $recordId")
            "STORY" -> database.storyDao().getById(recordId)?.let { firebase.syncStory(it) }
                ?: throw IllegalStateException("Local story record not found: $recordId")
            "KNOWLEDGE" -> database.knowledgeDao().getById(recordId)?.let { firebase.syncKnowledge(it) }
                ?: throw IllegalStateException("Local knowledge record not found: $recordId")
            "IMAGE" -> database.imageDao().getById(recordId)?.let { firebase.syncImage(it) }
                ?: throw IllegalStateException("Local image record not found: $recordId")
            else -> throw IllegalArgumentException("Unsupported record type: $recordType")
        }
    }

    companion object {
        private const val BATCH_SIZE = 20
        private const val MAX_BATCHES = 5
        private const val STALE_UPLOAD_MS = 15 * 60 * 1000L
        private const val BASE_BACKOFF_MS = 30_000L
        private const val MAX_BACKOFF_MS = 6 * 60 * 60 * 1000L
        private const val SECURITY_RETRY_MS = 60 * 60 * 1000L
    }
}
