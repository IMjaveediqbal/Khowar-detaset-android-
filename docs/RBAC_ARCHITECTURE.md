# Khowar Dataset RBAC Architecture

## Security model

RBAC is server-authoritative. The Android client may hide or show UI based on role, but it never grants permission. Firestore rules and Firebase Functions enforce authorization. A user's role is trusted only when assigned by privileged backend administration or Firebase Auth custom claims.

## Roles

| Role | Purpose | Core permissions |
|---|---|---|
| VISITOR | Unauthenticated/public user | Read released/approved public data; browse public community content |
| CONTRIBUTOR | Native speakers and data contributors | Create submissions, manage own drafts, view own contribution history, withdraw own consent |
| VALIDATOR | Community quality reviewer | Review eligible submissions, record validation decisions, flag quality issues; never review own records |
| EXPERT | Linguist/domain expert | Expert verification, resolve linguistic/cultural disputes, approve transition to research-ready |
| RESEARCHER | Approved research user | Access research-ready datasets according to license/approval, create API keys where permitted, export approved research data |
| MODERATOR | Community safety/quality | Moderate reports, community posts/comments, resolve abuse/content issues; no expert verification authority |
| ADMIN | Operational administrator | User/role administration, dataset governance, moderation oversight, release preparation, audit access |
| SUPER_ADMIN | Platform owner | Full administrative authority including role delegation, security configuration and final release |

## Permission matrix

| Permission | Visitor | Contributor | Validator | Expert | Researcher | Moderator | Admin | Super Admin |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Read public approved data | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Create dataset submission | | ✓ | ✓ | ✓ | | | ✓ | ✓ |
| Edit own draft | | ✓ | | | | | | |
| Submit for validation | | ✓ | | | | | ✓ | ✓ |
| Community validation | | | ✓ | ✓ | | | ✓ | ✓ |
| Expert verification | | | | ✓ | | | ✓ | ✓ |
| Resolve moderation | | | | | | ✓ | ✓ | ✓ |
| Research-ready access | | | | ✓ | ✓ | | ✓ | ✓ |
| Generate research API key | | | | | ✓ | | ✓ | ✓ |
| Release dataset version | | | | | | | ✓ | ✓ |
| Manage users | | | | | | | ✓ | ✓ |
| Assign Expert/Validator roles | | | | | | | ✓ | ✓ |
| Assign Admin role | | | | | | | | ✓ |
| Change security policy | | | | | | | | ✓ |
| Read audit logs | | | | | | | ✓ | ✓ |

## Dataset state machine

`RAW -> QUALITY_CHECKED -> COMMUNITY_VERIFIED -> EXPERT_VERIFIED -> RESEARCH_READY -> RELEASED`

Only trusted backend functions can perform state transitions. Each transition validates prerequisites and creates an audit record.

### Required controls

1. Authentication is required for every privileged operation.
2. Role is read from trusted claims and/or the server-side user profile; client-provided role values are ignored.
3. Contributors cannot validate or expert-verify their own records.
4. Validators cannot bypass the required submission state.
5. Experts cannot skip required validation, consent, metadata, provenance or moderation checks.
6. Research-ready data requires consent, licensing, provenance, metadata completeness and sufficient validation evidence.
7. RELEASED is an administrative operation and is never a client-side-only action.
8. Every privileged mutation records actor UID, role, action, target, previous state, new state, timestamp and reason in an audit log.
9. Role elevation follows least privilege and must itself be audited.
10. Revoked or suspended users immediately lose privileged access after their auth token/claims are refreshed.

## Role assignment lifecycle

- New accounts start as `CONTRIBUTOR` after authenticated onboarding unless explicitly configured otherwise.
- `VALIDATOR` and `EXPERT` are assigned only by Admin/Super Admin through trusted backend functions.
- `ADMIN` is assigned only by Super Admin.
- `SUPER_ADMIN` is bootstrap-only and should be tightly limited.
- Role changes are deny-by-default, auditable and should require a reason.

## Implementation boundaries

### Android

Android uses the role only for navigation, visibility and user feedback. It must never write privileged role fields or directly update protected dataset stages.

### Firestore Rules

Rules allow safe reads and contribution creation while denying direct client updates to protected dataset records. Privileged mutations remain backend-only.

### Firebase Functions

Callable functions enforce role checks, ownership, state transitions, validation prerequisites and audit logging. Functions should use Firebase Admin SDK and server timestamps.

### Firebase Auth

Custom claims should contain compact authorization attributes such as `role`, `disabled` and optionally organization/research scopes. Claims are managed only by privileged backend code.

## Recommended claims

```json
{
  "role": "EXPERT",
  "disabled": false,
  "researchApproved": true
}
```

Never trust a role supplied in a callable request body.

## Governance principle

The system follows **least privilege + separation of duties + server authority + auditability**. UI permissions are convenience; backend authorization is security.
