package com.example.data.sync

import android.content.Context
import com.example.data.local.SyncQueueDao
import com.example.data.local.SyncQueueEntry
import java.io.File
import java.util.UUID

/** Creates durable local outbox entries before any network operation. */
class OfflineSubmission(private val context: Context, private val queue: SyncQueueDao) {
    suspend fun enqueue(
        recordType: String,
        recordId: String = UUID.randomUUID().toString(),
        mediaSource: File? = null
    ): String {
        val localMedia = mediaSource?.let { source ->
            val mediaDir = File(context.filesDir, "offline_media").apply { mkdirs() }
            val destination = File(mediaDir, "${recordId}_${source.name}")
            source.copyTo(destination, overwrite = true)
            destination.absolutePath
        }

        queue.upsert(
            SyncQueueEntry(
                id = UUID.randomUUID().toString(),
                recordType = recordType,
                recordId = recordId,
                mediaPath = localMedia,
                status = SyncQueueEntry.PENDING
            )
        )
        return recordId
    }
}
