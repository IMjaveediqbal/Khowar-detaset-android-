# Khowar Dataset RBAC Architecture

## Security model
RBAC is deny-by-default and enforced in three layers:
1. Android UX (`Rbac`) hides actions and routes users to role workspaces. It is not a security boundary.
2. Firebase callable functions resolve trusted roles from Firebase Auth custom claims, falling back to `users/{uid}.role`, and enforce privileged operations.
3. Firestore blocks direct client dataset updates; trusted Cloud Functions perform privileged mutations and write audit logs.

## Authentication and account provisioning
There is **one common login flow** for every authenticated account. Roles are authorization levels, not separate login systems.

### Public registration
- Public self-registration never asks the user to choose a role.
- Every newly registered public account is created as `CONTRIBUTOR`.
- The contributor can submit words, sentences, speech, stories, knowledge and images and manage their own drafts/submissions.
- A public user cannot self-escalate to a privileged role.

### Privileged/staff accounts
The following roles are provisioned by an authorized administrator rather than selected during public registration:
- VALIDATOR
- EXPERT
- RESEARCHER
- MODERATOR
- DATA_STEWARD
- AUDITOR
- ADMIN
- SUPER_ADMIN

The preferred provisioning flow is:
`Admin creates/invites account → user sets their own password → user uses the common login → server loads trusted role → role dashboard opens.`

Administrators must not need to know a staff member's permanent password. If an environment requires a temporary password, it must be changed on first login.

### Role-driven experience
After authentication, the server-authoritative role determines the dashboard and permissions. There are no separate Contributor/Researcher/Expert/Admin login pages.

## Roles
- VISITOR — unauthenticated/public read-only state; retained in the model for compatibility.
- CONTRIBUTOR — public registered user; submit and manage own contributions.
- VALIDATOR — peer validation and quality review.
- EXPERT — linguistic/cultural expert verification.
- RESEARCHER — research-ready dataset/API access.
- MODERATOR — community safety, reports and moderation.
- DATA_STEWARD — metadata, data quality and governance.
- AUDITOR — audit history and oversight.
- ADMIN — operational user/dataset administration and release.
- SUPER_ADMIN — complete system authority.

## Permission matrix
| Permission | Allowed roles |
|---|---|
| CONTRIBUTE | CONTRIBUTOR, VALIDATOR, EXPERT, RESEARCHER, MODERATOR, ADMIN, SUPER_ADMIN |
| VALIDATE | VALIDATOR, EXPERT, ADMIN, SUPER_ADMIN |
| EXPERT_VERIFY | EXPERT, ADMIN, SUPER_ADMIN |
| RESEARCH_ACCESS | RESEARCHER, ADMIN, SUPER_ADMIN |
| MODERATE | MODERATOR, ADMIN, SUPER_ADMIN |
| MANAGE_USERS | ADMIN, SUPER_ADMIN |
| MANAGE_DATASET | ADMIN, SUPER_ADMIN |
| RELEASE_DATASET | ADMIN, SUPER_ADMIN |
| VIEW_AUDIT_LOGS | ADMIN, SUPER_ADMIN |

## Role boundaries
- Contributor is the default and only public self-registration role.
- Validator reviews community submissions but cannot assign privileged roles.
- Expert performs advanced linguistic/cultural verification; Expert is not automatically an administrator.
- Researcher consumes approved/research-ready data and API services; Researcher is not automatically an Expert.
- Moderator handles community reports and safety actions.
- Data Steward maintains metadata, quality and governance.
- Auditor reviews audit history without receiving normal mutation authority.
- Admin manages operational staff and dataset governance within the server policy.
- Super Admin controls Admin/Super Admin-level governance.

## Role assignment
Public registration always creates `CONTRIBUTOR`. Clients cannot choose a privileged role. `setUserRole` is the trusted role-management endpoint. ADMIN and SUPER_ADMIN can manage roles; ADMIN cannot grant ADMIN/SUPER_ADMIN, and only SUPER_ADMIN can grant SUPER_ADMIN. Role changes update Firebase Auth custom claims, the user profile, and an audit log.

A role assignment must never rely on a client-side role selector alone. The backend must verify the actor's authority, target account, requested role, and audit reason before changing the role.

## API access
API access uses the same authenticated account; it does not create a second login system. By default, API key generation is available to RESEARCHER, ADMIN and SUPER_ADMIN. Other roles require an explicit policy decision. API keys must be revocable, rate-limited and stored securely (hashed at rest; never depend on a client-visible plaintext key as the persisted secret).

## Dataset lifecycle
`RAW → QUALITY_CHECKED → COMMUNITY_VERIFIED → EXPERT_VERIFIED → RESEARCH_READY → RELEASED`

The server validates transitions, role authority, ownership/self-review restrictions, moderation holds, approval state, provenance, licensing, confidence and release authority.

## Governance
- Deny by default.
- No self-escalation.
- No self-validation or self-expert-verification.
- No direct client authorization of release.
- Privileged actions are audited.
- UI RBAC is advisory; backend RBAC is authoritative.
- Research release follows the existing research-readiness and expert-verification governance policies.
