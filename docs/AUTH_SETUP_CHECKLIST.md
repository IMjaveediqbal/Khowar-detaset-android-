# Firebase Auth Setup Checklist

Before releasing the authentication build:

- Enable Email/Password in Firebase Authentication.
- Enable Google provider in Firebase Authentication.
- Register the Android package `com.aistudio.khowardataset.vblzpq` in the Firebase project.
- Add the debug and release SHA-1/SHA-256 fingerprints to the Firebase Android app.
- Download the current `google-services.json` into the Android `app/` module.
- Confirm the generated `default_web_client_id` exists after the Google Services Gradle plugin runs.
- Deploy the trusted RBAC Firebase Functions used by `getMyRbac` and `setUserRole`.
- Ensure Firestore and Storage rules reject privileged writes based only on client-supplied role fields.
- Test: new account -> CONTRIBUTOR; sign out -> auth screen; sign in -> same account; password reset; Google sign-in; server role promotion; role refresh after restart; offline contribution queue.
