package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.FirebaseSyncService
import com.example.data.remote.RoomCloudSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KhowarApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this, appScope)
        RoomCloudSync(database, FirebaseSyncService()).start(appScope)
    }
}
