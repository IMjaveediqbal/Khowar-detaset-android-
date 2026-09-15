package com.example.security

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

/**
 * Creates a staff account without ever exposing a permanent password to an administrator.
 *
 * A secondary FirebaseAuth instance is used so the administrator's current session is
 * never replaced by the newly-created staff account. The generated password is random,
 * discarded after the reset email is requested, and cannot be recovered by the admin UI.
 */
class StaffInvitationService(context: Context) {
    private val appContext = context.applicationContext
    private val primaryApp = FirebaseApp.getInstance()

    suspend fun invite(email: String, displayName: String, role: UserRole): Result<Unit> = runCatching {
        require(role in setOf(
            UserRole.VALIDATOR,
            UserRole.EXPERT,
            UserRole.RESEARCHER,
            UserRole.MODERATOR,
            UserRole.DATA_STEWARD,
            UserRole.AUDITOR,
            UserRole.ADMIN
        )) { "Only staff roles can be invited." }

        val normalizedEmail = email.trim().lowercase()
        require(EMAIL_REGEX.matches(normalizedEmail)) { "Enter a valid staff email address." }
        require(displayName.trim().length in 2..100) { "Enter a name between 2 and 100 characters." }

        val secondaryName = "staff-provisioning-${UUID.randomUUID()}"
        val secondaryApp = FirebaseApp.initializeApp(appContext, primaryApp.options, secondaryName)
            ?: error("Unable to initialize staff provisioning Firebase app.")
        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
        var created = false

        try {
            val temporaryPassword = randomPassword()
            val result = secondaryAuth.createUserWithEmailAndPassword(normalizedEmail, temporaryPassword).await()
            val newUser = result.user ?: error("Firebase did not return the new account.")
            created = true
            newUser.updateProfile(userProfileChangeRequest { this.displayName = displayName.trim() }).await()

            // The reset email is the only supported password handoff to the staff member.
            secondaryAuth.sendPasswordResetEmail(normalizedEmail).await()
            secondaryAuth.signOut()

            // Primary admin session remains active and authorizes the privileged role assignment.
            RbacService().setUserRole(targetUid = newUser.uid, role = role).getOrThrow()
        } catch (error: Throwable) {
            if (created) runCatching { secondaryAuth.currentUser?.delete()?.await() }
            throw error
        } finally {
            runCatching { secondaryAuth.signOut() }
            runCatching { FirebaseApp.getInstance(secondaryName).delete().await() }
        }
    }

    private fun randomPassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%"
        val random = SecureRandom()
        return buildString(32) { repeat(32) { append(chars[random.nextInt(chars.length)]) } }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
