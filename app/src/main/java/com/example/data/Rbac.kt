package com.example.data

import com.example.data.model.UserRole

/** Client-side RBAC is UX protection only. Firebase callable functions remain authoritative. */
object Rbac {
    enum class Permission {
        CONTRIBUTE, VALIDATE, EXPERT_VERIFY, RESEARCH_ACCESS,
        MODERATE, MANAGE_USERS, MANAGE_DATASET, RELEASE_DATASET, VIEW_AUDIT_LOGS
    }

    private val permissions: Map<UserRole, Set<Permission>> = mapOf(
        UserRole.VISITOR to emptySet(),
        UserRole.CONTRIBUTOR to setOf(Permission.CONTRIBUTE),
        UserRole.VALIDATOR to setOf(Permission.CONTRIBUTE, Permission.VALIDATE),
        UserRole.EXPERT to setOf(Permission.CONTRIBUTE, Permission.VALIDATE, Permission.EXPERT_VERIFY),
        UserRole.RESEARCHER to setOf(Permission.CONTRIBUTE, Permission.RESEARCH_ACCESS),
        UserRole.MODERATOR to setOf(Permission.CONTRIBUTE, Permission.MODERATE),
        UserRole.ADMIN to setOf(
            Permission.CONTRIBUTE, Permission.VALIDATE, Permission.EXPERT_VERIFY,
            Permission.RESEARCH_ACCESS, Permission.MODERATE, Permission.MANAGE_USERS,
            Permission.MANAGE_DATASET, Permission.RELEASE_DATASET, Permission.VIEW_AUDIT_LOGS
        ),
        UserRole.SUPER_ADMIN to Permission.values().toSet()
    )

    fun can(role: UserRole, permission: Permission): Boolean = permissions[role]?.contains(permission) == true
    fun permissionsFor(role: UserRole): Set<Permission> = permissions[role].orEmpty()

    fun canManageRole(actor: UserRole, target: UserRole): Boolean = when (actor) {
        UserRole.SUPER_ADMIN -> target != UserRole.VISITOR
        UserRole.ADMIN -> target in setOf(
            UserRole.CONTRIBUTOR, UserRole.VALIDATOR, UserRole.EXPERT,
            UserRole.RESEARCHER, UserRole.MODERATOR
        )
        else -> false
    }
}
