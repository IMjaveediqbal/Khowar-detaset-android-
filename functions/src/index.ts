import { onCall, HttpsError } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

initializeApp();
const db = getFirestore();

/**
 * Trusted validator endpoint. The Android client can request a decision,
 * but only this backend is allowed to promote a record to APPROVED/REJECTED.
 * Set custom claim `validator: true` on trusted validator accounts.
 */
export const reviewSubmission = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }

  if (request.auth.token.validator !== true) {
    throw new HttpsError("permission-denied", "Validator role is required.");
  }

  const collection = String(request.data?.collection ?? "").trim();
  const recordId = String(request.data?.recordId ?? "").trim();
  const decision = String(request.data?.decision ?? "").trim().toUpperCase();
  const notes = String(request.data?.notes ?? "").trim();

  if (!collection || !recordId || !["APPROVED", "REJECTED"].includes(decision)) {
    throw new HttpsError("invalid-argument", "Invalid review request.");
  }

  const ref = db.collection(collection).doc(recordId);
  const snap = await ref.get();
  if (!snap.exists) {
    throw new HttpsError("not-found", "Submission not found.");
  }

  const data = snap.data() ?? {};
  if (data.status !== "SUBMITTED") {
    throw new HttpsError("failed-precondition", "Only submitted records can be reviewed.");
  }

  await db.runTransaction(async (tx) => {
    tx.update(ref, {
      status: decision,
      reviewedBy: request.auth!.uid,
      reviewedAt: FieldValue.serverTimestamp(),
      reviewNotes: notes,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return { ok: true, collection, recordId, status: decision };
});
