import { onCall, HttpsError } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

initializeApp();
const db = getFirestore();

export const reviewSubmission = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication is required.");
  if (request.auth.token.validator !== true) throw new HttpsError("permission-denied", "Validator role is required.");

  const collection = String(request.data?.collection ?? "").trim();
  const recordId = String(request.data?.recordId ?? "").trim();
  const decision = String(request.data?.decision ?? "").trim().toUpperCase();
  const notes = String(request.data?.notes ?? "").trim();
  if (!collection || !recordId || !["APPROVED", "REJECTED"].includes(decision)) {
    throw new HttpsError("invalid-argument", "Invalid review request.");
  }

  const ref = db.collection(collection).doc(recordId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError("not-found", "Submission not found.");
  if (snap.data()?.status !== "SUBMITTED") throw new HttpsError("failed-precondition", "Only submitted records can be reviewed.");

  await ref.update({
    status: decision,
    reviewedBy: request.auth.uid,
    reviewedAt: FieldValue.serverTimestamp(),
    reviewNotes: notes,
    updatedAt: FieldValue.serverTimestamp(),
  });
  return { ok: true, collection, recordId, status: decision };
});

export const voteOnCommunityPost = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication is required.");

  const postId = String(request.data?.postId ?? "").trim();
  if (!postId) throw new HttpsError("invalid-argument", "postId is required.");

  const postRef = db.collection("communityPosts").doc(postId);
  const voteRef = postRef.collection("votes").doc(request.auth.uid);

  await db.runTransaction(async (tx) => {
    const [post, vote] = await Promise.all([tx.get(postRef), tx.get(voteRef)]);
    if (!post.exists) throw new HttpsError("not-found", "Community post not found.");

    if (vote.exists) {
      tx.delete(voteRef);
      tx.update(postRef, { voteScore: FieldValue.increment(-1), updatedAt: FieldValue.serverTimestamp() });
    } else {
      tx.set(voteRef, { createdAt: FieldValue.serverTimestamp() });
      tx.update(postRef, { voteScore: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() });
    }
  });

  return { ok: true, postId };
});
