package com.example.data

import com.example.data.model.DataStage
import com.example.data.model.UserRole
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Client gateway for privileged RBAC operations. Authorization is performed again by Cloud Functions.
 */
class RbacService(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun setUserRole(targetUid: String, role: UserRole): Result<Unit> = runCatching {
        require(targetUid.isNotBlank()) { "Target user is required." }
        functions.getHttpsCallable("setUserRole")
            .call(mapOf("targetUid" to targetUid, "role" to role.name))
            .await()
    }.map { Unit }

    suspend fun transitionDataStage(
        collection: String,
        recordId: String,
        targetStage: DataStage,
        comments: String = "",
        confidenceScore: Int = 0
    ): Result<Unit> = runCatching {
        require(collection.isNotBlank()) { "Dataset collection is required." }
        require(recordId.isNotBlank()) { "Record ID is required." }
        require(comments.length <= 4000) { "Comments are too long." }
        require(confidenceScore in 0..5) { "Confidence score must be between 0 and 5." }
        functions.getHttpsCallable("transitionDataStage")
            .call(mapOf(
                "collection" to collection.trim().lowercase(),
                "recordId" to recordId.trim(),
                "targetStage" to targetStage.name,
                "comments" to comments.trim(),
                "confidenceScore" to confidenceScore
            ))
            .await()
    }.map { Unit }
}
