# Account Provisioning Policy

## Goal
Keep Khowar data collection simple for the public while keeping privileged roles controlled and auditable.

## Public account
A person who wants to contribute does not choose a role.

`Register → Firebase account → CONTRIBUTOR → Contributor workspace`

The registration UI must never expose a list of privileged roles.

## Staff account
Privileged roles are provisioned by an authorized administrator:

- VALIDATOR
- EXPERT
- RESEARCHER
- MODERATOR
- DATA_STEWARD
- AUDITOR
- ADMIN (SUPER_ADMIN only)

The implemented flow is:

`Admin → trusted provisionStaffAccount function → staff account + role → password-reset email → common login → trusted role loaded → staff workspace`

The Firebase Admin SDK creates the account without an administrator-supplied password. The Android client then requests Firebase's standard password-reset email for the staff email. The administrator never receives or stores the permanent password.

If the reset email is not received, the account remains provisioned and an administrator can provide a password-reset action again rather than recreating the account.

## Administrative account
`SUPER_ADMIN` provisions `ADMIN` accounts. `ADMIN` can provision operational staff roles but cannot grant `ADMIN` or `SUPER_ADMIN`. `SUPER_ADMIN` is the only role that can grant `SUPER_ADMIN` through the normal role-management path.

## One login
There is one authentication entry point for all authenticated users. A role changes authorization and workspace after login; it does not create a separate login system.

## Security requirements
1. Public registration always produces `CONTRIBUTOR`.
2. Client input cannot select or escalate a privileged role.
3. Staff account creation and role assignment are authorized by the backend, not by Android UI state.
4. Role changes and staff provisioning are audited.
5. Permanent passwords are never generated, displayed, or stored by administrators.
6. Staff establish their password through Firebase's password-reset flow.
7. API access is an authorization capability on the existing account, not a second login.
8. UI restrictions are advisory; Firebase Functions and Firestore rules are authoritative.
9. If provisioning succeeds but the reset-email request fails, the account is retained so the invitation can be retried safely.

## Implementation boundary
`functions/src/staffProvisioning.ts` contains the trusted account-provisioning callable. `functions/src/index2.ts` exposes it alongside the existing Functions exports. The Android `StaffInvitationService` calls that trusted endpoint and then requests the standard Firebase password-reset email without replacing the administrator's session.
