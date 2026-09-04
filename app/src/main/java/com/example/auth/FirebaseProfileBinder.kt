package com.example.auth

import com.example.data.local.AppDatabase
import com.example.data.model.User
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** Binds the authenticated Firebase identity to exactly one local contributor profile. */
class FirebaseProfileBinder(private val database: AppDatabase) {
    private val userDao = database.userDao()

    suspend fun bind(firebaseUser: FirebaseUser, displayName: String? = null, username: String? = null, region: String = "Chitral"): User =
        withContext(Dispatchers.IO) {
            require(firebaseUser.isEmailVerified) { "Email verification is required." }
            val uid = firebaseUser.uid
            val email = firebaseUser.email?.trim()?.lowercase()
                ?: throw IllegalArgumentException("Authenticated account has no email.")

            val byUid = userDao.getUserByFirebaseUid(uid)
            if (byUid != null) {
                require(byUid.email == email) { "Firebase identity/email mismatch." }
                return@withContext byUid
            }

            val byEmail = userDao.getUserByEmail(email)
            if (byEmail != null) {
                require(byEmail.firebaseUid == null || byEmail.firebaseUid == uid) {
                    "This contributor profile is already bound to another Firebase account."
                }
                val bound = byEmail.copy(firebaseUid = uid)
                userDao.update(bound)
                return@withContext bound
            }

            val fallbackName = email.substringBefore("@").ifBlank { "Contributor" }
            val newUser = User(
                id = UUID.randomUUID().toString(),
                email = email,
                displayName = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackName,
                username = username?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackName,
                role = UserRole.CONTRIBUTOR,
                region = region.trim().ifBlank { "Chitral" },
                preferredLanguage = "en",
                firebaseUid = uid
            )
            userDao.insert(newUser)
            newUser
        }
}
