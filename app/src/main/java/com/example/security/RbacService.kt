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

    suspend fun setUserRole(targetUid: String, role: UserRole): Result<Unit> = runCatching {
        require(auth.currentUser != null) { "Authentication is required." }
        require(targetUid.isNotBlank()) { "Target Firebase UID is required." }
        functions.getHttpsCallable("setUserRole")
            .call(mapOf("targetUid" to targetUid.trim(), "role" to role.name))
            .await()
        Unit
    }

    suspend fun transitionDataStage(
        collection: String,
        recordId: String,
        targetStage: String,
        comments: String,
        confidenceScore: Int
    ): Result<Unit> = runCatching {
        require(auth.currentUser != null) { "Authentication is required." }
        functions.getHttpsCallable("transitionDataStage")
            .call(mapOf(
                "collection" to collection.lowercase(),
                "recordId" to recordId.trim(),
                "targetStage" to targetStage.uppercase(),
                "comments" to comments.trim(),
                "confidenceScore" to confidenceScore
            ))
            .await()
        Unit
    }
}
