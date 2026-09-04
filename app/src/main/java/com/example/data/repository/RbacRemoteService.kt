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
    suspend fun setUserRole(targetUid: String, role: String): Result<Unit> = runCatching {
        require(targetUid.isNotBlank()) { "Target user is required." }
        functions.getHttpsCallable("setUserRole").call(
            mapOf("targetUid" to targetUid, "role" to role.uppercase())
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
