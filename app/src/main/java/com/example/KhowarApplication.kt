package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.FirebaseSyncService
import com.example.data.remote.RoomCloudSync
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KhowarApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this, appScope)

        // Firebase is optional until google-services.json is configured.
        // Initialize App Check immediately after Firebase so every Firebase
        // service created by the app can participate in attestation.
        val firebaseApp = FirebaseApp.initializeApp(this)
        if (firebaseApp != null) {
            val appCheck = FirebaseAppCheck.getInstance(firebaseApp)
            if (BuildConfig.DEBUG) {
                // Debug builds need a registered App Check debug token when
                // App Check enforcement is enabled for development/CI.
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                // Production uses Play Integrity. The Android app must also
                // be registered with Play Integrity in Firebase App Check.
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }

            RoomCloudSync(database, FirebaseSyncService()).start(appScope)
        }
    }
}
