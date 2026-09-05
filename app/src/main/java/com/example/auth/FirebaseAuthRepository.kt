package com.example.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun observeAuthState(listener: (FirebaseUser?) -> Unit) =
        auth.addAuthStateListener { listener(it.currentUser) }

    fun removeAuthStateListener(listener: (FirebaseAuth) -> Unit) =
        auth.removeAuthStateListener(listener)

    suspend fun signUp(email: String, password: String, displayName: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Firebase did not return a user account.")
        if (displayName.isNotBlank()) {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
            ).await()
        }
        return user
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        return auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Firebase did not return a user session.")
    }

    suspend fun signInWithGoogleIdToken(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential).await().user
            ?: error("Firebase did not return a user session.")
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() = auth.signOut()
}
