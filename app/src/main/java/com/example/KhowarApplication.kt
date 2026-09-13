package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.DatasetSyncWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
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
            AppCheckInstaller.install(appCheck)

            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            auth.addAuthStateListener { DatasetSyncWorker.syncNow(this) }
            if (auth.currentUser == null) auth.signInAnonymously()
            DatasetSyncWorker.schedule(this)
        }
    }
}
