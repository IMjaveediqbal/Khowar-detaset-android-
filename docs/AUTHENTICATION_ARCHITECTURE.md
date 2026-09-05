# Khowar Dataset Authentication Architecture

## Identity

Firebase Authentication is the source of truth for account identity. The Firebase UID is used as the local Room `User.id` for authenticated accounts.

## Roles

Every public registration starts as `CONTRIBUTOR`. The client never allows a user to select `VALIDATOR`, `RESEARCHER`, `ADMIN`, or another privileged role.

Privileged roles are assigned by the trusted Firebase RBAC backend and refreshed into the local Room cache after authentication.

## Authentication methods

- Email/password sign-in
- Email/password account creation
- Google Identity Credential Manager sign-in
- Password reset email
- Persistent Firebase session
- Explicit sign-out

## Startup flow

1. Firebase restores the authenticated session.
2. `AuthGate` blocks the application UI until identity is known.
3. A Room profile is created/updated using the Firebase UID.
4. Trusted RBAC permissions are refreshed from the server.
5. The normal Khowar Dataset UI is displayed.

## Security boundary

UI visibility is not the security boundary. Firebase callable functions, Firestore rules, and Storage rules must enforce privileged operations server-side. Local Room roles are only a cache used for UX/navigation.

## Offline behavior

Authentication remains Firebase-based. Once authenticated, dataset contribution remains backed by the existing Room/offline queue architecture; synchronization can resume when network connectivity returns.
