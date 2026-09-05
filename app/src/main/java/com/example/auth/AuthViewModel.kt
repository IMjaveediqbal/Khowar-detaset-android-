package com.example.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = FirebaseAuthRepository()
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        _state.value = if (user == null) AuthState.SignedOut else AuthState.SignedIn(user)
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Enter your email and password.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            runCatching { repository.signIn(email, password) }
                .onFailure { _state.value = AuthState.Error(firebaseMessage(it)) }
        }
    }

    fun signUp(name: String, email: String, password: String, confirmPassword: String) {
        when {
            name.isBlank() -> _state.value = AuthState.Error("Enter your full name.")
            email.isBlank() -> _state.value = AuthState.Error("Enter your email address.")
            password.length < 8 -> _state.value = AuthState.Error("Use a password of at least 8 characters.")
            password != confirmPassword -> _state.value = AuthState.Error("Passwords do not match.")
            else -> viewModelScope.launch {
                _state.value = AuthState.Loading
                runCatching { repository.signUp(email, password, name) }
                    .onFailure { _state.value = AuthState.Error(firebaseMessage(it)) }
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _state.value = AuthState.Error("Enter your email address first.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            runCatching { repository.sendPasswordReset(email) }
                .onSuccess { _state.value = AuthState.Error("Password reset email sent.") }
                .onFailure { _state.value = AuthState.Error(firebaseMessage(it)) }
        }
    }

    fun signOut() = repository.signOut()

    private fun firebaseMessage(t: Throwable): String = when {
        t.message?.contains("password is invalid", ignoreCase = true) == true -> "Incorrect email or password."
        t.message?.contains("no user record", ignoreCase = true) == true -> "No account exists for this email."
        t.message?.contains("email address is already in use", ignoreCase = true) == true -> "An account already exists for this email."
        t.message?.contains("badly formatted", ignoreCase = true) == true -> "Enter a valid email address."
        else -> t.message ?: "Authentication failed. Please try again."
    }

    override fun onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        super.onCleared()
    }
}
