package com.example.data.auth

import com.example.data.model.Permission
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/** Network boundary for privileged RBAC operations. The server is authoritative. */
class RbacService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    private fun requireAuth() { check(auth.currentUser != null) { "Authentication is required." } }

    suspend fun getAuthorization(): Authorization = runCatching {
        requireAuth()
        val result = functions.getHttpsCallable("getMyAuthorization").call().await()
        val data = result.data as? Map<*, *> ?: error("Invalid authorization response.")
        val role = runCatching { UserRole.valueOf(data["role"].toString().uppercase()) }.getOrDefault(UserRole.CONTRIBUTOR)
        val permissions = (data["permissions"] as? List<*>)
            ?.mapNotNull { runCatching { Permission.valueOf(it.toString()) }.getOrNull() }
            ?.toSet().orEmpty()
        Authorization(auth.currentUser!!.uid, role, permissions)
    }.getOrElse { Authorization(auth.currentUser?.uid, UserRole.CONTRIBUTOR, setOf(Permission.READ_PUBLIC_DATASET)) }

    suspend fun transitionDataStage(collection: String, recordId: String, targetStage: String, comments: String = "", confidenceScore: Int = 0): Result<Unit> = runCatching {
        requireAuth()
        require(collection.isNotBlank()) { "Collection is required." }
        require(recordId.isNotBlank()) { "Record ID is required." }
        require(comments.length <= 4000) { "Comments are too long." }
        require(confidenceScore in 0..5) { "Confidence score must be between 0 and 5." }
        functions.getHttpsCallable("transitionDataStage").call(mapOf(
            "collection" to collection.trim().lowercase(), "recordId" to recordId.trim(),
            "targetStage" to targetStage.trim().uppercase(), "comments" to comments.trim(), "confidenceScore" to confidenceScore
        )).await()
        Unit
    }

    suspend fun setUserRole(targetUid: String, role: String, reason: String): Result<Unit> = runCatching {
        requireAuth()
        require(targetUid.isNotBlank()) { "User UID is required." }
        require(role.trim().uppercase() in RbacServerRoles.ALL) { "Invalid role." }
        require(reason.trim().length in 5..1000) { "A role-change reason of 5–1000 characters is required." }
        functions.getHttpsCallable("setUserRole").call(mapOf(
            "targetUid" to targetUid.trim(), "role" to role.trim().uppercase(), "reason" to reason.trim()
        )).await()
        Unit
    }
}

data class Authorization(val uid: String?, val role: UserRole, val permissions: Set<Permission>) {
    fun can(permission: Permission): Boolean = permission in permissions
}

private object RbacServerRoles {
    val ALL = setOf("VISITOR", "CONTRIBUTOR", "VALIDATOR", "EXPERT", "RESEARCHER", "MODERATOR", "DATA_STEWARD", "AUDITOR", "ADMIN", "SUPER_ADMIN")
}
