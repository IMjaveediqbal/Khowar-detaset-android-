package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Durable outbox entry for offline-first dataset synchronization. */
@Entity(tableName = "sync_queue")
data class SyncQueueEntry(
    @PrimaryKey val id: String,
    val recordType: String,
    val recordId: String,
    val mediaPath: String? = null,
    val status: String = PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val PENDING = "PENDING"
        const val UPLOADING = "UPLOADING"
        const val SYNCED = "SYNCED"
        const val FAILED = "FAILED"
    }
}
