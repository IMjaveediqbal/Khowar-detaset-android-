package com.example.security

import com.example.data.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RbacPolicyTest {
    @Test fun contributorCannotValidateOrManageUsers() {
        assertFalse(RbacPolicy.can(UserRole.CONTRIBUTOR, RbacPermission.VALIDATE_COMMUNITY))
        assertFalse(RbacPolicy.can(UserRole.CONTRIBUTOR, RbacPermission.MANAGE_USERS))
    }

    @Test fun validatorCanValidateButNotRelease() {
        assertTrue(RbacPolicy.can(UserRole.VALIDATOR, RbacPermission.VALIDATE_COMMUNITY))
        assertTrue(RbacPolicy.can(UserRole.VALIDATOR, RbacPermission.VERIFY_EXPERT))
        assertFalse(RbacPolicy.can(UserRole.VALIDATOR, RbacPermission.RELEASE_DATASET))
    }

    @Test fun expertCanResearchButCannotRelease() {
        assertTrue(RbacPolicy.can(UserRole.EXPERT, RbacPermission.VERIFY_EXPERT))
        assertTrue(RbacPolicy.can(UserRole.EXPERT, RbacPermission.ACCESS_RESEARCH_HUB))
        assertFalse(RbacPolicy.can(UserRole.EXPERT, RbacPermission.RELEASE_DATASET))
    }

    @Test fun researcherCannotModerate() {
        assertTrue(RbacPolicy.can(UserRole.RESEARCHER, RbacPermission.ACCESS_RESEARCH_HUB))
        assertFalse(RbacPolicy.can(UserRole.RESEARCHER, RbacPermission.MODERATE_COMMUNITY))
    }

    @Test fun onlyAdministratorsRelease() {
        assertTrue(RbacPolicy.can(UserRole.ADMIN, RbacPermission.RELEASE_DATASET))
        assertTrue(RbacPolicy.can(UserRole.SUPER_ADMIN, RbacPermission.MANAGE_SECURITY))
        assertFalse(RbacPolicy.can(UserRole.MODERATOR, RbacPermission.RELEASE_DATASET))
    }
}
