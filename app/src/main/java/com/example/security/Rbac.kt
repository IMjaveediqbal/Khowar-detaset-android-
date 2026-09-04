package com.example.security

import com.example.data.model.UserRole

/**
 * Central client-side RBAC vocabulary.
 *
 * This is intentionally a UX guard only. Every privileged operation must still
 * be authorized by Firebase Authentication + trusted Cloud Functions.
 */
enum class Permission {
    VIEW_PUBLIC_DATA,
    CONTRIBUTE_DATA,
    EDIT_OWN_DRAFTS,
    VALIDATE_SUBMISSIONS,
    EXPERT_VERIFY_DATA,
    ACCESS_RESEARCH_DATA,
    EXPORT_DATA,
    GENERATE_API_KEYS,
    MODERATE_COMMUNITY,
    RESOLVE_MODERATION,
    MANAGE_USERS,
    MANAGE_ROLES,
    MANAGE_DATASET_RELEASES,
    RELEASE_DATASET,
    VIEW_AUDIT_LOGS,
    MANAGE_SYSTEM_POLICY
}

object Rbac {
    private val permissions: Map<UserRole, Set<Permission>> = mapOf(
        UserRole.VISITOR to setOf(Permission.VIEW_PUBLIC_DATA),
        UserRole.CONTRIBUTOR to setOf(
            Permission.VIEW_PUBLIC_DATA,
            Permission.CONTRIBUTE_DATA,
            Permission.EDIT_OWN_DRAFTS
        ),
        UserRole.VALIDATOR to setOf(
            Permission.VIEW_PUBLIC_DATA,
            Permission.CONTRIBUTE_DATA,
            Permission.EDIT_OWN_DRAFTS,
            Permission.VALIDATE_SUBMISSIONS
        ),
        UserRole.EXPERT to setOf(
            Permission.VIEW_PUBLIC_DATA,
            Permission.CONTRIBUTE_DATA,
            Permission.EDIT_OWN_DRAFTS,
            Permission.VALIDATE_SUBMISSIONS,
            Permission.EXPERT_VERIFY_DATA,
            Permission.ACCESS_RESEARCH_DATA
        ),
        UserRole.RESEARCHER to setOf(
            Permission.VIEW_PUBLIC_DATA,
            Permission.ACCESS_RESEARCH_DATA,
            Permission.EXPORT_DATA,
            Permission.GENERATE_API_KEYS
        ),
        UserRole.MODERATOR to setOf(
            Permission.VIEW_PUBLIC_DATA,
            Permission.MODERATE_COMMUNITY,
            Permission.RESOLVE_MODERATION
        ),
        UserRole.ADMIN to setOf(
            Permission.VIEW_PUBLIC_DATA,
            Permission.CONTRIBUTE_DATA,
            Permission.EDIT_OWN_DRAFTS,
            Permission.VALIDATE_SUBMISSIONS,
            Permission.EXPERT_VERIFY_DATA,
            Permission.ACCESS_RESEARCH_DATA,
            Permission.EXPORT_DATA,
            Permission.GENERATE_API_KEYS,
            Permission.MODERATE_COMMUNITY,
            Permission.RESOLVE_MODERATION,
            Permission.MANAGE_USERS,
            Permission.MANAGE_ROLES,
            Permission.MANAGE_DATASET_RELEASES,
            Permission.RELEASE_DATASET,
            Permission.VIEW_AUDIT_LOGS
        ),
        UserRole.SUPER_ADMIN to Permission.values().toSet()
    )

    fun has(role: UserRole, permission: Permission): Boolean = permissions[role]?.contains(permission) == true

    fun permissionsFor(role: UserRole): Set<Permission> = permissions[role].orEmpty()

    fun canAssignRole(actor: UserRole, target: UserRole): Boolean = when (actor) {
        UserRole.SUPER_ADMIN -> true
        UserRole.ADMIN -> target != UserRole.SUPER_ADMIN && target != UserRole.ADMIN
        else -> false
    }

    fun canManageUser(actor: UserRole): Boolean = has(actor, Permission.MANAGE_USERS)
}
