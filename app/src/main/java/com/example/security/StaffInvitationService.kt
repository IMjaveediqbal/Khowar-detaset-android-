package com.example.security

import android.content.Context
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Client gateway for staff onboarding.
 * Account creation and privileged role assignment happen in Firebase Functions.
 * The administrator never creates, receives, or stores a permanent password.
 */
class StaffInvitationService(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance()

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
        require(auth.currentUser != null) { "Authentication is required." }

        val result = functions.getHttpsCallable("provisionStaffAccount").call(
            mapOf(
                "email" to normalizedEmail,
                "displayName" to displayName.trim(),
                "role" to role.name,
                "reason" to "Staff account provisioned by administrator."
            )
        ).await()

        val data = result.data as? Map<*, *> ?: error("Invalid provisioning response.")
        require(data["ok"] == true) { "Staff account provisioning failed." }

        // Firebase's standard password-reset email is the password handoff.
        // The admin session is unchanged because this call does not sign in the target user.
        auth.sendPasswordResetEmail(normalizedEmail).await()
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
