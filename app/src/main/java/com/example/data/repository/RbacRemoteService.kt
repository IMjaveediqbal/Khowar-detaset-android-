package com.example.data.repository

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Client gateway for privileged RBAC operations.
 * Authorization is performed by Firebase Functions, never by this class.
 */
class RbacRemoteService(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun getMyRbac(): Result<Pair<String, String>> = runCatching {
        val result = functions.getHttpsCallable("getMyRbac").call().await()
        val data = result.data as? Map<*, *>
            ?: error("Invalid RBAC response.")
        val uid = data["uid"]?.toString().orEmpty()
        val role = data["role"]?.toString()?.uppercase().orEmpty()
        require(uid.isNotBlank() && role.isNotBlank()) { "Invalid RBAC identity." }
        uid to role
    }

    suspend fun setUserRole(
        targetUid: String,
        role: String,
        reason: String
    ): Result<Unit> = runCatching {
        require(targetUid.isNotBlank()) { "Target user is required." }
        require(reason.trim().length >= 5) { "A role-change reason is required." }
        functions.getHttpsCallable("setUserRole").call(
            mapOf(
                "targetUid" to targetUid,
                "role" to role.uppercase(),
                "reason" to reason.trim()
            )
        ).await()
        Unit
    }

    suspend fun transitionDataStage(
        collection: String,
        recordId: String,
        targetStage: String,
        comments: String = "",
        confidenceScore: Int = 0
    ): Result<Unit> = runCatching {
        require(collection.isNotBlank()) { "Dataset collection is required." }
        require(recordId.isNotBlank()) { "Record ID is required." }
        functions.getHttpsCallable("transitionDataStage").call(
            mapOf(
                "collection" to collection.lowercase(),
                "recordId" to recordId,
                "targetStage" to targetStage.uppercase(),
                "comments" to comments,
                "confidenceScore" to confidenceScore
            )
        ).await()
        Unit
    }
}