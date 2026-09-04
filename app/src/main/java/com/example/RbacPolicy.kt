package com.example

import com.example.data.model.UserRole

enum class RbacPermission {
    READ_PUBLIC_DATA,
    CREATE_DATASET_SUBMISSION,
    EDIT_OWN_DRAFT,
    SUBMIT_FOR_VALIDATION,
    VALIDATE_COMMUNITY,
    EXPERT_VERIFICATION,
    MODERATE_CONTENT,
    ACCESS_RESEARCH_HUB,
    GENERATE_RESEARCH_API_KEY,
    RELEASE_DATASET,
    MANAGE_USERS,
    ASSIGN_VALIDATOR_EXPERT,
    ASSIGN_ADMIN,
    MANAGE_SECURITY_POLICY,
    READ_AUDIT_LOGS
}

object RbacPolicy {
    private val matrix: Map<UserRole, Set<RbacPermission>> = mapOf(
        UserRole.VISITOR to setOf(RbacPermission.READ_PUBLIC_DATA),
        UserRole.CONTRIBUTOR to setOf(
            RbacPermission.READ_PUBLIC_DATA,
            RbacPermission.CREATE_DATASET_SUBMISSION,
            RbacPermission.EDIT_OWN_DRAFT,
            RbacPermission.SUBMIT_FOR_VALIDATION
        ),
        UserRole.VALIDATOR to setOf(
            RbacPermission.READ_PUBLIC_DATA,
            RbacPermission.CREATE_DATASET_SUBMISSION,
            RbacPermission.VALIDATE_COMMUNITY
        ),
        UserRole.EXPERT to setOf(
            RbacPermission.READ_PUBLIC_DATA,
            RbacPermission.CREATE_DATASET_SUBMISSION,
            RbacPermission.VALIDATE_COMMUNITY,
            RbacPermission.EXPERT_VERIFICATION,
            RbacPermission.ACCESS_RESEARCH_HUB
        ),
        UserRole.RESEARCHER to setOf(
            RbacPermission.READ_PUBLIC_DATA,
            RbacPermission.ACCESS_RESEARCH_HUB,
            RbacPermission.GENERATE_RESEARCH_API_KEY
        ),
        UserRole.MODERATOR to setOf(
            RbacPermission.READ_PUBLIC_DATA,
            RbacPermission.MODERATE_CONTENT
        ),
        UserRole.ADMIN to setOf(
            RbacPermission.READ_PUBLIC_DATA,
            RbacPermission.CREATE_DATASET_SUBMISSION,
            RbacPermission.SUBMIT_FOR_VALIDATION,
            RbacPermission.VALIDATE_COMMUNITY,
            RbacPermission.EXPERT_VERIFICATION,
            RbacPermission.MODERATE_CONTENT,
            RbacPermission.ACCESS_RESEARCH_HUB,
            RbacPermission.GENERATE_RESEARCH_API_KEY,
            RbacPermission.RELEASE_DATASET,
            RbacPermission.MANAGE_USERS,
            RbacPermission.ASSIGN_VALIDATOR_EXPERT,
            RbacPermission.READ_AUDIT_LOGS
        ),
        UserRole.SUPER_ADMIN to RbacPermission.values().toSet()
    )

    fun can(role: UserRole?, permission: RbacPermission): Boolean =
        role != null && permission in matrix[role].orEmpty()

    fun permissions(role: UserRole?): Set<RbacPermission> = matrix[role].orEmpty()
}
