package com.example.data.auth

import com.example.data.model.UserRole

enum class Permission {
    VIEW_PUBLIC_DATASET,
    CONTRIBUTE_DATA,
    REVIEW_COMMUNITY,
    VERIFY_EXPERT,
    MANAGE_MODERATION,
    ACCESS_RESEARCH_HUB,
    EXPORT_DATASET,
    MANAGE_USERS,
    MANAGE_ROLES,
    CREATE_RELEASE,
    RELEASE_DATASET,
    VIEW_AUDIT_LOGS
}

/**
 * Single client-side RBAC policy used for navigation and UX gating.
 * Security is enforced independently by Firebase Authentication/Cloud Functions.
 */
object Rbac {
    private val permissionsByRole = mapOf(
        UserRole.VISITOR to setOf(Permission.VIEW_PUBLIC_DATASET),
        UserRole.CONTRIBUTOR to setOf(Permission.VIEW_PUBLIC_DATASET, Permission.CONTRIBUTE_DATA),
        UserRole.VALIDATOR to setOf(
            Permission.VIEW_PUBLIC_DATASET, Permission.CONTRIBUTE_DATA,
            Permission.REVIEW_COMMUNITY, Permission.VERIFY_EXPERT
        ),
        UserRole.EXPERT to setOf(
            Permission.VIEW_PUBLIC_DATASET, Permission.CONTRIBUTE_DATA,
            Permission.REVIEW_COMMUNITY, Permission.VERIFY_EXPERT,
            Permission.MANAGE_MODERATION
        ),
        UserRole.RESEARCHER to setOf(
            Permission.VIEW_PUBLIC_DATASET, Permission.CONTRIBUTE_DATA,
            Permission.ACCESS_RESEARCH_HUB, Permission.EXPORT_DATASET
        ),
        UserRole.MODERATOR to setOf(
            Permission.VIEW_PUBLIC_DATASET, Permission.CONTRIBUTE_DATA,
            Permission.REVIEW_COMMUNITY, Permission.MANAGE_MODERATION
        ),
        UserRole.ADMIN to setOf(
            Permission.VIEW_PUBLIC_DATASET, Permission.CONTRIBUTE_DATA,
            Permission.REVIEW_COMMUNITY, Permission.VERIFY_EXPERT,
            Permission.MANAGE_MODERATION, Permission.ACCESS_RESEARCH_HUB,
            Permission.EXPORT_DATASET, Permission.MANAGE_USERS,
            Permission.MANAGE_ROLES, Permission.CREATE_RELEASE,
            Permission.RELEASE_DATASET, Permission.VIEW_AUDIT_LOGS
        ),
        UserRole.SUPER_ADMIN to Permission.values().toSet()
    )

    fun has(role: UserRole?, permission: Permission): Boolean =
        role != null && permission in (permissionsByRole[role] ?: emptySet())

    fun canManageRole(actor: UserRole?, target: UserRole): Boolean {
        if (actor == UserRole.SUPER_ADMIN) return true
        if (actor != UserRole.ADMIN) return false
        return target !in setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)
    }

    fun label(role: UserRole): String = when (role) {
        UserRole.VISITOR -> "Visitor"
        UserRole.CONTRIBUTOR -> "Contributor"
        UserRole.VALIDATOR -> "Community Validator"
        UserRole.EXPERT -> "Language Expert"
        UserRole.RESEARCHER -> "Researcher"
        UserRole.MODERATOR -> "Moderator"
        UserRole.ADMIN -> "Administrator"
        UserRole.SUPER_ADMIN -> "Super Administrator"
    }
}
