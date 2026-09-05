package com.example.auth

import com.google.firebase.auth.FirebaseUser

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: FirebaseUser) : AuthState
    data class Error(val message: String) : AuthState
}
