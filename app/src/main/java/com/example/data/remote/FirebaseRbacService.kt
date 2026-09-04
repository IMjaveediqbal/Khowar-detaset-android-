package com.example.data.remote

import com.example.data.model.DataStage
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Trusted-role client gateway. The Android app requests privileged operations;
 * Firebase Functions remains the authority that grants/denies them.
 */
class FirebaseRbacService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    private fun requireAuthenticated() {
        check(auth.currentUser != null) { "Authentication is required." }
    }

    suspend fun transitionDataStage(
        collection: String,
        recordId: String,
        targetStage: DataStage,
        comments: String = "",
        confidenceScore: Int = 0
    ): Result<Unit> = runCatching {
        requireAuthenticated()
        require(collection.trim().isNotEmpty()) { "Collection is required." }
        require(recordId.trim().isNotEmpty()) { "Record ID is required." }
        require(comments.length <= 4000) { "Comments are too long." }
        require(confidenceScore in 0..5) { "Confidence score must be 0–5." }

        functions.getHttpsCallable("transitionDataStage").call(
            mapOf(
                "collection" to collection.trim().lowercase(),
                "recordId" to recordId.trim(),
                "targetStage" to targetStage.name,
                "comments" to comments.trim(),
                "confidenceScore" to confidenceScore
            )
        ).await()
        Unit
    }

    suspend fun setUserRole(targetUid: String, role: UserRole): Result<Unit> = runCatching {
        requireAuthenticated()
        require(targetUid.trim().isNotEmpty()) { "Target user is required." }
        require(role != UserRole.VISITOR) { "VISITOR is reserved for unauthenticated/read-only state." }

        functions.getHttpsCallable("setUserRole").call(
            mapOf(
                "targetUid" to targetUid.trim(),
                "role" to role.name
            )
        ).await()
        Unit
    }
}
