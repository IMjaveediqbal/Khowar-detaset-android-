package com.example.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/** Firebase-backed account authentication. Passwords are handled only by Firebase Auth. */
class AuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser get() = auth.currentUser

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        require(email.trim().isNotEmpty()) { "Email is required." }
        require(password.length >= 8) { "Password must be at least 8 characters." }
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.sendEmailVerification()?.await()
    }

    suspend fun signIn(email: String, password: String): Result<Boolean> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        auth.currentUser?.isEmailVerified == true
    }

    suspend fun resendVerificationEmail(): Result<Unit> = runCatching {
        require(auth.currentUser != null) { "No signed-in account." }
        auth.currentUser?.sendEmailVerification()?.await()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        require(email.trim().isNotEmpty()) { "Email is required." }
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() = auth.signOut()
}
