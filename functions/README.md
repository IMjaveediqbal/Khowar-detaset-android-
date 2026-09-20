# Khowar Dataset Cloud Validator

The `reviewSubmission` callable function is the trusted path for changing a cloud submission from `SUBMITTED` to `APPROVED` or `REJECTED`.

## Account and role access

Public registration creates only `CONTRIBUTOR` profiles. Privileged access uses the protected `role` custom claim and matching `users/{uid}.role` value; user-controlled clients cannot write either field. Existing administrators enter through `khowardataset://admin/login`. The administrator provisioning callable always creates `ADMIN`, and `SUPER_ADMIN` remains bootstrap-only outside the app.

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
