package com.example.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()
    val state by authViewModel.state.collectAsStateCompat()
    when (state) {
        AuthState.SignedOut -> AuthScreen(authViewModel)
        is AuthState.SignedIn -> content()
        AuthState.Loading -> AuthLoadingScreen()
        is AuthState.Error -> AuthScreen(authViewModel)
    }
}

@Composable
private fun AuthLoadingScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) { androidx.compose.material3.CircularProgressIndicator() }
}

@Composable
private fun AuthViewModel.collectAsStateCompat() = kotlinx.coroutines.flow.collectAsStateWithLifecycleCompat(state)
