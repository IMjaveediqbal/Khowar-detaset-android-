# Khowar Dataset Android

Android app for community contributions of Khowar words, sentences, speech, stories, cultural knowledge and images. Kotlin/Compose UI, Room local storage, Firebase Authentication, Firestore, Storage and callable Functions.

## Development

1. Install JDK 21 (required for Robolectric tests targeting Android 16), Android SDK Platform 36 and Build Tools 36.0.0. Set `ANDROID_HOME` or `sdk.dir` in untracked `local.properties`.
2. Run `./gradlew testDebugUnitTest lintDebug compileReleaseKotlin assembleDebug`.
3. Firebase is optional for launching the offline shell. To use connected features, register the application ID from `app/build.gradle.kts` in Firebase and download `app/google-services.json`. This file is ignored by Git.
4. Enable Email/Password and Anonymous Authentication. Anonymous accounts browse approved records; contributions require an email/password account. Creating an account while browsing anonymously links that identity. App Check debug builds require a registered debug token; release builds use Play Integrity.
5. Use a separate Firebase development project. With Node 20+ and Java 21, run `npm ci --prefix functions` and `npm test --prefix functions`. Run `npm run test:integration --prefix functions` for isolated `demo-khowar` emulator tests.
6. Deploy Functions, Firestore rules/indexes and Storage rules together to your development project using Firebase CLI. Enable the cross-service Firestore access required by Storage rules. Never deploy just the new Android client against the old rules/functions.
7. Provision the first SUPER_ADMIN using the Admin SDK from an authorized administrative environment. Both the protected `/users/{uid}.role` and custom claim should agree. No account can self-assign privileges.

Release signing uses `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` and alias `upload`. Do not commit a release keystore. Debug builds use Android's generated debug keystore.

## Data flow

- Sign-in is handled by Firebase Authentication. Room user IDs are Firebase UIDs. Editing profile text does not authenticate or change an email address.
- Submission commits a local record, consent, audit entry and upload operation in one Room transaction.
- WorkManager drains queued operations over a connected network, with exponential retry and visible upload states. Cloud submission is idempotent for the record ID/payload/owner.
- All six collections download in pages of 100. Approved and owned records are available to contributors; trusted reviewers can retrieve pending work. Withdrawn previously public records emit tombstones for other clients.
- Media is uploaded under `{collection}/{uid}/{recordId}`, without public token URLs. Reviewed media cannot be overwritten. Downloads use authenticated Storage SDK access.
- Review and lifecycle transitions run in backend transactions. Review history and audit entries are written on the server. Research and expert transitions are separate from initial approval.
- Consent withdrawal stops pending local uploads and archives cloud records. Bulk withdrawal is resumable; retry the action after a network error. Formerly downloaded copies cannot be recalled.
- Exports include all six collection types that are approved, consented and research-ready/released. JSON includes a schema version, count and JSONL checksum. CSV embeds the complete JSON record to preserve type-specific metadata. Export packages are saved using Android's system file picker.
- Immutable draft releases are created on the backend. The pilot limit is 100 records per collection and 700 KB of snapshot JSON; larger release creation explicitly fails and requires a background export pipeline. Draft creation does not publish a release.

## Research and product boundaries

- Local linguistic suggestions are explicitly heuristic; they are not a trained Khowar model. No automatic linguistic verification is performed.
- Research HTTP API and API tokens are planned; the UI does not issue fake credentials or advertise a live endpoint.
- Stable speaker pseudonyms currently represent the signed-in contributor. The voice form must be used for that contributor's own voice. Multiple speakers under one collector account need a dedicated speaker registry before field collection.
- Keep original text. Normalization is for search, and must be reviewed by native-language experts. Dialect variants and homonyms are allowed; duplicate matches are advisory.
- Existing local UUID profiles are not automatically claimed by email: that would permit impersonation. Preserve the old database and perform a supervised migration of legacy contributions to verified identities.
- Existing cloud records without `consentGranted`, trusted `ownerUid`, `dataStage` and valid media metadata need an administrative migration. They are not automatically eligible for research export.
- Locale direction and existing translated strings are supported. New workflow copy and Khowar linguistic accuracy require native-speaker editorial QA before launch.
- WorkManager background sync is periodic, not an instant push feed. The profile's Sync action requests immediate work. UI queries search the local corpus in SQLite and load 200 matching records per type at a time with a Load more action. Very large corpora can add FTS to accelerate substring search.

## Verification before a pilot

Use distinct contributor, validator and expert accounts on two devices. Record offline, reconnect, observe a confirmed upload, review from the second device, advance the research stage, export and withdraw. Confirm no revoked record appears in a subsequent export and that a different contributor cannot review or download private recordings. Test microphone denial, process recreation, retries, account changes, RTL, large text and release App Check.

See `IMPLEMENTATION_NOTES.md` for verification evidence and remaining rollout requirements.
