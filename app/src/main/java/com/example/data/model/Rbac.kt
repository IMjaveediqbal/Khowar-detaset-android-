package com.example.data.model

/**
 * Application RBAC contract. UI visibility is convenience only; every privileged
 * operation must also be enforced by Firebase callable functions/security rules.
 */
enum class Permission {
    READ_PUBLIC_DATASET,
    SUBMIT_DATA,
    REVIEW_SUBMISSIONS,
    EXPERT_VERIFY,
    MANAGE_MODERATION,
    ACCESS_RESEARCH_DATA,
    GENERATE_API_KEYS,
    MANAGE_USERS,
    MANAGE_ROLES,
    RELEASE_DATASET,
    VIEW_AUDIT_LOGS,
    MANAGE_SYSTEM
}

object RbacPolicy {
    private val rolePermissions: Map<UserRole, Set<Permission>> = mapOf(
        UserRole.VISITOR to setOf(Permission.READ_PUBLIC_DATASET),
        UserRole.CONTRIBUTOR to setOf(
            Permission.READ_PUBLIC_DATASET,
            Permission.SUBMIT_DATA
        ),
        UserRole.VALIDATOR to setOf(
            Permission.READ_PUBLIC_DATASET,
            Permission.SUBMIT_DATA,
            Permission.REVIEW_SUBMISSIONS
        ),
        UserRole.EXPERT to setOf(
            Permission.READ_PUBLIC_DATASET,
            Permission.SUBMIT_DATA,
            Permission.REVIEW_SUBMISSIONS,
            Permission.EXPERT_VERIFY
        ),
        UserRole.RESEARCHER to setOf(
            Permission.READ_PUBLIC_DATASET,
            Permission.ACCESS_RESEARCH_DATA,
            Permission.GENERATE_API_KEYS
        ),
        UserRole.MODERATOR to setOf(
            Permission.READ_PUBLIC_DATASET,
            Permission.SUBMIT_DATA,
            Permission.MANAGE_MODERATION
        ),
        UserRole.ADMIN to Permission.values().toSet() - Permission.MANAGE_SYSTEM,
        UserRole.SUPER_ADMIN to Permission.values().toSet()
    )

    fun permissions(role: UserRole): Set<Permission> = rolePermissions[role].orEmpty()

    fun has(role: UserRole, permission: Permission): Boolean = permission in permissions(role)

    fun canAssignRole(actor: UserRole, target: UserRole): Boolean = when (actor) {
        UserRole.SUPER_ADMIN -> true
        UserRole.ADMIN -> target !in setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)
        else -> false
    }
}
