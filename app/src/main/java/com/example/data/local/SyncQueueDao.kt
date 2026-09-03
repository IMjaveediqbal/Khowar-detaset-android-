package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncQueueEntry>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SyncQueueEntry>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun pendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'FAILED'")
    fun failedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'SYNCED'")
    fun syncedCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SyncQueueEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SyncQueueEntry)

    @Query("UPDATE sync_queue SET status = :status, retryCount = :retryCount, lastError = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, retryCount: Int, error: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)
}
