# Authentication Test Plan

## Email/password

- Empty fields are rejected.
- Invalid email is rejected.
- Passwords shorter than 8 characters are rejected during signup.
- Mismatched confirmation is rejected.
- Existing email returns a clear error.
- Wrong password returns a generic safe error.
- Successful signup opens the authenticated application.
- Successful signup creates a local profile with the Firebase UID and CONTRIBUTOR role.

## Session

- Restarting the app restores the Firebase session.
- Signed-out users cannot reach the dataset UI.
- Sign-out returns to the authentication screen.

## Google

- Google credential picker opens.
- A valid Google ID token is exchanged with Firebase.
- Google-created users receive a local Firebase-UID profile and CONTRIBUTOR role.
- Cancelled/failed Google sign-in does not open the app as an authenticated user.

## RBAC

- New users never choose privileged roles.
- Server-assigned roles replace the local role cache after refresh.
- Validator, Researcher, and Admin navigation remains protected.
- Privileged operations remain server-authorized even if the local role cache is tampered with.

## Offline

- Existing local contribution queues remain available after authentication.
- Pending contributions do not disappear when the device goes offline.
- Sync resumes after connectivity returns.
