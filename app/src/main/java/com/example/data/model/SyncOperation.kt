package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Persistent outbox entry. It survives process death and device restarts. */
@Entity(
    tableName = "sync_operations",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["state", "nextAttemptAt"]),
        Index(value = ["ownerFirebaseUid"])
    ]
)
data class SyncOperation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idempotencyKey: String,
    val recordType: String,
    val recordId: String,
    val operationType: String = "CREATE",
    val ownerFirebaseUid: String,
    val state: String = "PENDING",
    val attempts: Int = 0,
    val lastError: String? = null,
    val nextAttemptAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
