package com.example

import com.example.data.model.UserRole

enum class Permission {
    READ_PUBLIC_DATA, CREATE_DATASET_SUBMISSION, EDIT_OWN_DRAFT, SUBMIT_FOR_VALIDATION,
    COMMUNITY_VALIDATION, EXPERT_VERIFICATION, MODERATE_CONTENT, RESEARCH_ACCESS,
    GENERATE_RESEARCH_API_KEY, RELEASE_DATASET, MANAGE_USERS, ASSIGN_VALIDATOR_EXPERT,
    ASSIGN_ADMIN, MANAGE_SECURITY_POLICY, READ_AUDIT_LOGS
}

object RbacPolicy {
    private val matrix = mapOf(
        UserRole.VISITOR to setOf(Permission.READ_PUBLIC_DATA),
        UserRole.CONTRIBUTOR to setOf(Permission.READ_PUBLIC_DATA, Permission.CREATE_DATASET_SUBMISSION, Permission.EDIT_OWN_DRAFT, Permission.SUBMIT_FOR_VALIDATION),
        UserRole.VALIDATOR to setOf(Permission.READ_PUBLIC_DATA, Permission.CREATE_DATASET_SUBMISSION, Permission.COMMUNITY_VALIDATION),
        UserRole.EXPERT to setOf(Permission.READ_PUBLIC_DATA, Permission.CREATE_DATASET_SUBMISSION, Permission.COMMUNITY_VALIDATION, Permission.EXPERT_VERIFICATION, Permission.RESEARCH_ACCESS),
        UserRole.RESEARCHER to setOf(Permission.READ_PUBLIC_DATA, Permission.RESEARCH_ACCESS, Permission.GENERATE_RESEARCH_API_KEY),
        UserRole.MODERATOR to setOf(Permission.READ_PUBLIC_DATA, Permission.MODERATE_CONTENT),
        UserRole.ADMIN to Permission.values().toSet() - setOf(Permission.ASSIGN_ADMIN, Permission.MANAGE_SECURITY_POLICY),
        UserRole.SUPER_ADMIN to Permission.values().toSet()
    )

    fun can(role: UserRole?, permission: Permission): Boolean = role != null && permission in matrix[role].orEmpty()
    fun permissions(role: UserRole?): Set<Permission> = matrix[role].orEmpty()
}
