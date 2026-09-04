package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.RoomCloudSync
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KhowarApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this, appScope)

        // Firebase is optional until google-services.json is configured.
        // Room remains the local source of truth; WorkManager uploads when connected.
        val firebaseApp = FirebaseApp.initializeApp(this)
        if (firebaseApp != null) {
            RoomCloudSync(this, database).start(appScope)
        }
    }
}
