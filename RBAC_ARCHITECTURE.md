# Khowar Dataset RBAC Architecture

## Security model
RBAC is deny-by-default and enforced in three layers:
1. Android UX (`Rbac`) hides actions and routes users to role workspaces. It is not a security boundary.
2. Firebase callable functions resolve trusted roles from Firebase Auth custom claims, falling back to `users/{uid}.role`, and enforce privileged operations.
3. Firestore blocks direct client dataset updates; trusted Cloud Functions perform privileged mutations and write audit logs.

## Roles
- VISITOR — public/read-only experience.
- CONTRIBUTOR — submit words, sentences, speech, stories, knowledge and images.
- VALIDATOR — peer validation and quality review.
- EXPERT — linguistic/cultural expert verification.
- RESEARCHER — research-ready dataset/API access.
- MODERATOR — community safety, reports and moderation.
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

## Dataset lifecycle
`RAW → QUALITY_CHECKED → COMMUNITY_VERIFIED → EXPERT_VERIFIED → RESEARCH_READY → RELEASED`

The server validates transitions, role authority, ownership/self-review restrictions, moderation holds, approval state, provenance, licensing, confidence and release authority.

## Role assignment
Public registration always creates CONTRIBUTOR. Clients cannot choose a privileged role. `setUserRole` is the trusted role-management endpoint. ADMIN and SUPER_ADMIN can manage roles; ADMIN cannot grant ADMIN/SUPER_ADMIN, and only SUPER_ADMIN can grant SUPER_ADMIN. Role changes update Firebase Auth custom claims, the user profile, and an audit log.

## Governance
- Deny by default.
- No self-escalation.
- No self-validation or self-expert-verification.
- No direct client authorization of release.
- Privileged actions are audited.
- UI RBAC is advisory; backend RBAC is authoritative.
- Research release follows the existing research-readiness and expert-verification governance policies.
