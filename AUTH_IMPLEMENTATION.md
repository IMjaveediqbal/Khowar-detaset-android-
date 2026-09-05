# KDA Authentication Implementation

The Android app now has a Firebase Authentication layer for email/password accounts, password reset, sign-out, and a clean authentication screen.

## Role policy

- New self-registered accounts are Contributors.
- Validator, Researcher, Admin, Expert, and Super Admin roles must be assigned by the trusted backend.
- The Android client must never accept a role from a signup form as authority.

## Google Sign-In

Enable Google as a Firebase Authentication provider, add the Android app's SHA-1/SHA-256 fingerprints in Firebase, and provide the generated Web client ID to the Android project before enabling the Google credential launcher.

## Production requirements

1. Configure Firebase Authentication providers.
2. Deploy the trusted Cloud Functions used by `RbacRemoteService` (`getMyRbac` and `setUserRole`).
3. Enforce the same role policy in Firestore/Storage security rules and Functions.
4. Keep Room as the offline queue/cache and synchronize after authentication is restored.
5. Never store Firebase passwords or privileged role state locally as an authority.

## Current branch

`feature/firebase-auth-rbac-login`
