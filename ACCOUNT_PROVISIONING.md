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

Recommended flow:

`Admin invitation → user sets password → common login → trusted role loaded → staff workspace`

The administrator should not need to know the staff member's permanent password.

## Administrative account
`SUPER_ADMIN` provisions `ADMIN` accounts. `ADMIN` can provision operational staff roles but cannot grant `ADMIN` or `SUPER_ADMIN`. `SUPER_ADMIN` is the only role that can grant `SUPER_ADMIN`.

## One login
There is one authentication entry point for all authenticated users. A role changes authorization and workspace after login; it does not create a separate login system.

## Security requirements
1. Public registration always produces `CONTRIBUTOR`.
2. Client input cannot select or escalate a privileged role.
3. Backend role assignment verifies the acting administrator and target account.
4. Role changes update trusted authorization state and are audited.
5. Temporary credentials must be changed on first use.
6. Permanent passwords are never displayed to administrators.
7. API access is an authorization capability on the existing account, not a second login.
8. UI restrictions are advisory; Firebase Functions and Firestore rules are authoritative.

## Recommended future implementation
Add an explicit staff-invitation/provisioning flow to the Admin workspace. It should create or invite a Firebase Authentication identity, assign the requested role only after server-side authorization, and record an audit event. Do not implement staff creation by asking an administrator to enter or store the staff member's permanent password.
