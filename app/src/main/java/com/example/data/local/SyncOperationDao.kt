package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SyncOperation

@Dao
interface SyncOperationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(operation: SyncOperation)

    @Query("SELECT * FROM sync_operations WHERE state IN ('PENDING','FAILED') AND nextAttemptAt <= :now ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getDue(now: Long, limit: Int): List<SyncOperation>

    @Query("UPDATE sync_operations SET state='UPLOADING', updatedAt=:now WHERE id=:id AND state IN ('PENDING','FAILED')")
    suspend fun markUploading(id: String, now: Long): Int

    @Query("UPDATE sync_operations SET state='COMPLETED', lastError=NULL, updatedAt=:now WHERE id=:id")
    suspend fun markCompleted(id: String, now: Long)

    @Query("UPDATE sync_operations SET state='FAILED', attempts=attempts+1, lastError=:error, nextAttemptAt=:nextAttemptAt, updatedAt=:now WHERE id=:id")
    suspend fun markFailed(id: String, error: String, nextAttemptAt: Long, now: Long)

    @Query("UPDATE sync_operations SET state='PENDING', updatedAt=:now WHERE state='UPLOADING' AND updatedAt < :staleBefore")
    suspend fun recoverStale(staleBefore: Long, now: Long): Int

    @Query("DELETE FROM sync_operations WHERE state='COMPLETED' AND updatedAt < :before")
    suspend fun purgeCompleted(before: Long): Int
}
