package com.example.auth

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.viewmodel.KhowarViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()
    val khowarViewModel: KhowarViewModel = viewModel()
    val state by authViewModel.state.collectAsStateCompat()
    var profileReady by remember { mutableStateOf(false) }

    when (val current = state) {
        AuthState.SignedOut -> {
            profileReady = false
            AuthScreen(authViewModel)
        }
        is AuthState.SignedIn -> {
            LaunchedEffect(current.user.uid) {
                profileReady = false
                val profile = FirebaseProfileBootstrap.ensure(
                    context = getApplicationContextCompat(),
                    firebaseUser = current.user
                )
                khowarViewModel.repository.setCurrentUser(profile)
                khowarViewModel.refreshTrustedRole()
                profileReady = true
            }
            if (profileReady) content() else AuthLoadingScreen()
        }
        AuthState.Loading -> AuthLoadingScreen()
        is AuthState.Error -> AuthScreen(authViewModel)
    }
}

@Composable
private fun getApplicationContextCompat(): Context =
    androidx.compose.ui.platform.LocalContext.current.applicationContext

@Composable
private fun AuthLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthViewModel.collectAsStateCompat() =
    kotlinx.coroutines.flow.collectAsState(state)

private object FirebaseProfileBootstrap {
    suspend fun ensure(context: Context, firebaseUser: FirebaseUser): User {
        val db = AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val dao = db.userDao()
        val existing = dao.getById(firebaseUser.uid)
        if (existing != null) {
            val refreshed = existing.copy(
                email = firebaseUser.email ?: existing.email,
                displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: existing.displayName
            )
            dao.update(refreshed)
            return refreshed
        }

        val legacy = firebaseUser.email?.trim()?.lowercase()?.let { dao.getUserByEmail(it) }
        val profile = User(
            id = firebaseUser.uid,
            email = firebaseUser.email?.trim()?.lowercase() ?: "",
            displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: legacy?.displayName ?: "Khowar Contributor",
            username = legacy?.username
                ?: firebaseUser.email?.substringBefore("@")?.lowercase()?.replace(Regex("[^a-z0-9_]+"), "_")
                ?: "contributor_${firebaseUser.uid.take(8)}",
            role = UserRole.CONTRIBUTOR,
            preferredLanguage = legacy?.preferredLanguage ?: "en",
            region = legacy?.region ?: "Chitral",
            bio = legacy?.bio ?: "",
            isPublicProfile = legacy?.isPublicProfile ?: true,
            createdAt = legacy?.createdAt ?: System.currentTimeMillis()
        )
        dao.insert(profile)
        return profile
    }
}
