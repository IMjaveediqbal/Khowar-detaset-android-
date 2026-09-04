package com.example.data.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.SyncOperation
import com.example.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

/** Adds local changes to a durable outbox and asks WorkManager to drain it when online. */
class SyncScheduler(
    private val context: Context,
    private val database: AppDatabase,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun enqueue(recordType: String, recordId: String, operationType: String = "CREATE") {
        val user = auth.currentUser ?: throw IllegalStateException("Sign in before queueing cloud sync.")
        require(user.isEmailVerified) { "Verify your email before queueing cloud sync." }
        val key = "${user.uid}:$recordType:$recordId:$operationType"
        database.syncOperationDao().insert(
            SyncOperation(
                id = UUID.randomUUID().toString(),
                idempotencyKey = key,
                recordType = recordType,
                recordId = recordId,
                operationType = operationType,
                ownerFirebaseUid = user.uid
            )
        )
        schedule()
    }

    fun schedule() {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val WORK_NAME = "khowar-cloud-sync"
    }
}
