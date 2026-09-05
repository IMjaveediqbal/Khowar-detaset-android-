package com.example.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthGateFixed(content: @Composable () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()
    val state by authViewModel.state.collectAsState()
    when (state) {
        AuthState.SignedOut -> AuthScreen(authViewModel)
        is AuthState.SignedIn -> content()
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is AuthState.Error -> AuthScreen(authViewModel)
    }
}
