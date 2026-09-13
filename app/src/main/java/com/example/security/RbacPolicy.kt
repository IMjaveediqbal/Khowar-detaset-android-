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
    private val mapping = mapOf(
        RbacPermission.VIEW_PUBLIC_DATA to com.example.data.model.Permission.READ_PUBLIC_DATASET,
        RbacPermission.CONTRIBUTE_DATA to com.example.data.model.Permission.CREATE_CONTRIBUTION,
        RbacPermission.VALIDATE_COMMUNITY to com.example.data.model.Permission.VALIDATE_COMMUNITY,
        RbacPermission.VERIFY_EXPERT to com.example.data.model.Permission.EXPERT_VERIFY,
        RbacPermission.ACCESS_RESEARCH_HUB to com.example.data.model.Permission.EXPORT_RESEARCH_DATA,
        RbacPermission.MODERATE_COMMUNITY to com.example.data.model.Permission.MANAGE_MODERATION,
        RbacPermission.MANAGE_DATASET to com.example.data.model.Permission.MANAGE_METADATA,
        RbacPermission.MANAGE_USERS to com.example.data.model.Permission.MANAGE_USERS,
        RbacPermission.MANAGE_SECURITY to com.example.data.model.Permission.MANAGE_SYSTEM,
        RbacPermission.RELEASE_DATASET to com.example.data.model.Permission.RELEASE_DATASET,
        RbacPermission.VIEW_AUDIT_LOGS to com.example.data.model.Permission.VIEW_AUDIT_LOGS
    )
    fun can(role: UserRole?, permission: RbacPermission): Boolean =
        com.example.data.model.RbacPolicy.can(role ?: UserRole.VISITOR, mapping.getValue(permission))
    fun hasRole(role: UserRole?, vararg allowed: UserRole): Boolean = role != null && role in allowed
    fun isPrivileged(role: UserRole?): Boolean = hasRole(role, UserRole.VALIDATOR, UserRole.EXPERT, UserRole.RESEARCHER, UserRole.MODERATOR, UserRole.ADMIN, UserRole.SUPER_ADMIN)
    fun isAdministrative(role: UserRole?): Boolean = hasRole(role, UserRole.ADMIN, UserRole.SUPER_ADMIN)
}
