package com.example.security

import com.example.data.model.DataStage
import com.example.data.model.UserRole
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/** Trusted authorization gateway used by privileged Android workflows. */
class FirebaseRbacService(private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()) {
    suspend fun assignRole(targetUid: String, role: UserRole): Result<Unit> = runCatching {
        functions.getHttpsCallable("setUserRole")
            .call(mapOf("targetUid" to targetUid, "role" to role.name)).await()
        Unit
    }

    suspend fun transitionDataStage(collection: String, recordId: String, targetStage: DataStage, comments: String = "", confidenceScore: Int = 0): Result<Unit> = runCatching {
        functions.getHttpsCallable("transitionDataStage")
            .call(mapOf("collection" to collection, "recordId" to recordId, "targetStage" to targetStage.name, "comments" to comments, "confidenceScore" to confidenceScore)).await()
        Unit
    }
}
