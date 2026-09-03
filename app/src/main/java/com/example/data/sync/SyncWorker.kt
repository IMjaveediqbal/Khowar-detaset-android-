package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.SyncQueueEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Durable synchronization worker. It intentionally owns only queue state here;
 * dataset-specific uploaders can be registered without changing the queue model.
 * A failed item is retained locally and retried with WorkManager backoff.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext, kotlinx.coroutines.CoroutineScope(Dispatchers.IO))
        val queue = database.syncQueueDao()
        val pending = queue.getPending()

        if (pending.isEmpty()) return@withContext Result.success()

        var hasRetryableFailure = false

        for (item in pending) {
            try {
                queue.updateStatus(item.id, SyncQueueEntry.UPLOADING, item.retryCount, null)

                // The record-specific repository is responsible for uploading the
                // local record/media using item.recordType and item.recordId.
                // Until an uploader is registered, leave the item retryable rather
                // than deleting user data.
                throw UnsupportedOperationException("No dataset uploader registered for ${item.recordType}")
            } catch (t: Throwable) {
                hasRetryableFailure = true
                queue.updateStatus(
                    item.id,
                    SyncQueueEntry.FAILED,
                    item.retryCount + 1,
                    t.message ?: t::class.java.simpleName
                )
            }
        }

        if (hasRetryableFailure) Result.retry() else Result.success()
    }
}
