package com.example.security

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/** Trusted backend gateway for privileged RBAC operations. */
class RbacService(private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()) {
    suspend fun setUserRole(targetUid: String, role: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("setUserRole")
            .call(mapOf("targetUid" to targetUid, "role" to role.uppercase()))
            .await()
        Unit
    }

    suspend fun transitionDataStage(collection: String, recordId: String, targetStage: String, comments: String, confidenceScore: Int): Result<Unit> = runCatching {
        functions.getHttpsCallable("transitionDataStage")
            .call(mapOf(
                "collection" to collection.lowercase(),
                "recordId" to recordId,
                "targetStage" to targetStage.uppercase(),
                "comments" to comments,
                "confidenceScore" to confidenceScore
            ))
            .await()
        Unit
    }
}
