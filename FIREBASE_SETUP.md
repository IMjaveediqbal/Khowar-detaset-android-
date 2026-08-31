# Firebase setup for Khowar Dataset

The Android app now saves contributions locally in Room and mirrors submitted records to Firebase in the background.

## One-time Firebase setup

1. Create/select a Firebase project.
2. Add the Android app using application ID `com.aistudio.khowardataset.vblzpq`.
3. Download `google-services.json` and place it in `app/google-services.json`.
4. In Firebase Authentication, enable **Anonymous** sign-in. The app uses an anonymous Firebase identity for cloud submission until a full account-linking flow is added.
5. Create a **Cloud Firestore** database.
6. Create a **Cloud Storage** bucket.
7. Deploy the repository rules with the Firebase CLI:

   `firebase deploy --only firestore:rules,storage`

8. Test with the Firebase Emulator Suite or the Rules Playground before opening the service to public contributors.

## Data flow

`Android UI -> Room -> RoomCloudSync -> Firebase Authentication -> Firestore/Storage`

Room remains the local source of truth. Firestore provides the shared cloud copy. Firestore's Android SDK also supports offline persistence and synchronizes queued changes when connectivity returns.

## Cloud policy

- New mobile submissions are uploaded with status `SUBMITTED`.
- Client-side users cannot directly approve, edit, or delete cloud records through the included Firestore rules.
- Approved/public records should be promoted by a trusted validator/admin backend in a later phase.
- Audio is stored in Firebase Storage; Firestore stores its metadata and cloud URL.
- Images are stored in Firebase Storage; Firestore stores their metadata and cloud URL.

## Important

Do not commit `google-services.json` if your repository policy treats Firebase configuration as private. If your Firebase project uses App Check or stricter authentication, configure those services before public release.
