package com.example.security

import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/** Trusted backend gateway for privileged RBAC operations. */
class RbacService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun getMyRole(): Result<UserRole> = runCatching {
        require(auth.currentUser != null) { "Authentication is required." }
        val result = functions.getHttpsCallable("getMyRbac").call().await()
        val roleName = (result.data as? Map<*, *>)?.get("role")?.toString()?.uppercase()
            ?: error("Server did not return a role.")
        UserRole.valueOf(roleName)
    }

    suspend fun setUserRole(targetUid: String? = null, targetEmail: String? = null, role: UserRole): Result<Unit> = runCatching {
        require(auth.currentUser != null) { "Authentication is required." }
        val data = mutableMapOf<String, Any>("role" to role.name, "reason" to "Administrator approved role assignment")
        if (!targetUid.isNullOrBlank()) data["targetUid"] = targetUid.trim()
        if (!targetEmail.isNullOrBlank()) data["targetEmail"] = targetEmail.trim().lowercase()
        require(data.containsKey("targetUid") || data.containsKey("targetEmail")) { "Target Firebase UID or email is required." }
        functions.getHttpsCallable("setUserRole").call(data).await()
        Unit
    }

    suspend fun provisionManagedAccount(
        email: String,
        temporaryPassword: String,
        displayName: String,
        region: String
    ): Result<String> = runCatching {
        require(auth.currentUser != null) { "Authentication is required." }
        val result = functions.getHttpsCallable("provisionManagedAccount").call(mapOf(
            "email" to email.trim().lowercase(),
            "temporaryPassword" to temporaryPassword,
            "displayName" to displayName.trim(),
            "region" to region.trim()
        )).await()
        val data = result.data as? Map<*, *> ?: error("Server returned an invalid response.")
        data["email"]?.toString() ?: email.trim().lowercase()
    }

    suspend fun transitionDataStage(
        collection: String,
        recordId: String,
        targetStage: String,
        comments: String,
        confidenceScore: Int
    ): Result<Unit> = runCatching {
        require(auth.currentUser != null) { "Authentication is required." }
        functions.getHttpsCallable("transitionDataStage").call(mapOf(
            "collection" to collection.lowercase(),
            "recordId" to recordId.trim(),
            "targetStage" to targetStage.uppercase(),
            "comments" to comments.trim(),
            "confidenceScore" to confidenceScore
        )).await()
        Unit
    }
}
