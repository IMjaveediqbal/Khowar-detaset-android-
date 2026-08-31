# Khowar Dataset Cloud Validator

The `reviewSubmission` callable function is the trusted path for changing a cloud submission from `SUBMITTED` to `APPROVED` or `REJECTED`.

## Validator access

Validator accounts must have the Firebase Auth custom claim:

```json
{ "validator": true }
```

Do not put this claim under user-controlled Firestore profile data.

## Deploy

From the repository root after Firebase CLI authentication:

```bash
cd functions
npm install
npm run build
cd ..
firebase deploy --only functions
```

Test with the Firebase Emulator Suite before production deployment.
