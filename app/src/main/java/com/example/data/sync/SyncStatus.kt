package com.example.data.sync

import com.example.data.local.SyncQueueDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class SyncStatus(
    val pending: Int,
    val failed: Int,
    val synced: Int
) {
    val totalWaiting: Int get() = pending + failed
}

class SyncStatusRepository(private val dao: SyncQueueDao) {
    fun observe(): Flow<SyncStatus> = combine(
        dao.pendingCount(),
        dao.failedCount(),
        dao.syncedCount()
    ) { pending, failed, synced -> SyncStatus(pending, failed, synced) }
}
