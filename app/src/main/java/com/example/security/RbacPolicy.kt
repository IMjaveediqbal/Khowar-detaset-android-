package com.example.security

import com.example.data.model.UserRole

enum class RbacPermission {
    VIEW_PUBLIC_DATA,
    CONTRIBUTE_DATA,
    VALIDATE_COMMUNITY,
    VERIFY_EXPERT,
    ACCESS_RESEARCH_HUB,
    MODERATE_COMMUNITY,
    MANAGE_DATASET,
    MANAGE_USERS,
    MANAGE_SECURITY,
    RELEASE_DATASET,
    VIEW_AUDIT_LOGS
}

/** Client-side policy controls visibility/navigation only. Firebase Functions are authoritative. */
object RbacPolicy {
    private val permissions = mapOf(
        UserRole.VISITOR to setOf(RbacPermission.VIEW_PUBLIC_DATA),
        UserRole.CONTRIBUTOR to setOf(RbacPermission.VIEW_PUBLIC_DATA, RbacPermission.CONTRIBUTE_DATA),
        UserRole.VALIDATOR to setOf(RbacPermission.VIEW_PUBLIC_DATA, RbacPermission.CONTRIBUTE_DATA, RbacPermission.VALIDATE_COMMUNITY),
        UserRole.EXPERT to setOf(RbacPermission.VIEW_PUBLIC_DATA, RbacPermission.CONTRIBUTE_DATA, RbacPermission.VALIDATE_COMMUNITY, RbacPermission.VERIFY_EXPERT, RbacPermission.ACCESS_RESEARCH_HUB),
        UserRole.RESEARCHER to setOf(RbacPermission.VIEW_PUBLIC_DATA, RbacPermission.ACCESS_RESEARCH_HUB),
        UserRole.MODERATOR to setOf(RbacPermission.VIEW_PUBLIC_DATA, RbacPermission.CONTRIBUTE_DATA, RbacPermission.MODERATE_COMMUNITY),
        UserRole.ADMIN to setOf(RbacPermission.VIEW_PUBLIC_DATA, RbacPermission.CONTRIBUTE_DATA, RbacPermission.VALIDATE_COMMUNITY, RbacPermission.VERIFY_EXPERT, RbacPermission.ACCESS_RESEARCH_HUB, RbacPermission.MODERATE_COMMUNITY, RbacPermission.MANAGE_DATASET, RbacPermission.MANAGE_USERS, RbacPermission.MANAGE_SECURITY, RbacPermission.RELEASE_DATASET, RbacPermission.VIEW_AUDIT_LOGS),
        UserRole.SUPER_ADMIN to RbacPermission.entries.toSet()
    )

    fun can(role: UserRole?, permission: RbacPermission): Boolean = role != null && permission in (permissions[role] ?: emptySet())
    fun hasRole(role: UserRole?, vararg allowed: UserRole): Boolean = role != null && role in allowed
    fun isPrivileged(role: UserRole?): Boolean = hasRole(role, UserRole.VALIDATOR, UserRole.EXPERT, UserRole.RESEARCHER, UserRole.MODERATOR, UserRole.ADMIN, UserRole.SUPER_ADMIN)
    fun isAdministrative(role: UserRole?): Boolean = hasRole(role, UserRole.ADMIN, UserRole.SUPER_ADMIN)
}
