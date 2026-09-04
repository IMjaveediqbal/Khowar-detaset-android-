import { onCall, HttpsError } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

initializeApp();
const db = getFirestore();

const DATASET_COLLECTIONS = new Set([
  "lexicon", "sentences", "speech", "stories", "knowledge", "images"
]);

const STAGES = [
  "RAW",
  "QUALITY_CHECKED",
  "COMMUNITY_VERIFIED",
  "EXPERT_VERIFIED",
  "RESEARCH_READY",
  "RELEASED",
] as const;

type DatasetStage = typeof STAGES[number];

const ALLOWED_TRANSITIONS: Record<DatasetStage, DatasetStage[]> = {
  RAW: ["QUALITY_CHECKED"],
  QUALITY_CHECKED: ["COMMUNITY_VERIFIED"],
  COMMUNITY_VERIFIED: ["EXPERT_VERIFIED"],
  EXPERT_VERIFIED: ["RESEARCH_READY"],
  RESEARCH_READY: ["RELEASED"],
  RELEASED: [],
};

const getTrustedRole = async (uid: string, token: Record<string, unknown>) => {
  const claimRole = typeof token.role === "string" ? token.role.toUpperCase() : "";
  if (claimRole) return claimRole;

  const user = await db.collection("users").doc(uid).get();
  const role = user.data()?.role;
  return typeof role === "string" ? role.toUpperCase() : "";
};

const hasUnresolvedModeration = async (recordId: string) => {
  const snapshot = await db.collection("moderationReports")
    .where("recordId", "==", recordId)
    .where("status", "in", ["OPEN", "PENDING", "UNDER_REVIEW"])
    .limit(1)
    .get();
  return !snapshot.empty;
};

export const reviewSubmission = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication is required.");
  if (request.auth.token.validator !== true) throw new HttpsError("permission-denied", "Validator role is required.");

  const collection = String(request.data?.collection ?? "").trim().toLowerCase();
  const recordId = String(request.data?.recordId ?? "").trim();
  const decision = String(request.data?.decision ?? "").trim().toUpperCase();
  const notes = String(request.data?.notes ?? "").trim();

  if (!DATASET_COLLECTIONS.has(collection) || !recordId || !["APPROVED", "REJECTED"].includes(decision)) {
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

/**
 * Server-authoritative dataset lifecycle transition.
 * The Android client may request a transition, but it can never authorize one.
 */
export const transitionDataStage = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication is required.");

  const collection = String(request.data?.collection ?? "").trim().toLowerCase();
  const recordId = String(request.data?.recordId ?? "").trim();
  const targetStage = String(request.data?.targetStage ?? "").trim().toUpperCase() as DatasetStage;
  const comments = String(request.data?.comments ?? "").trim();
  const confidenceScore = Number(request.data?.confidenceScore ?? 0);

  if (!DATASET_COLLECTIONS.has(collection) || !recordId) {
    throw new HttpsError("invalid-argument", "A valid dataset collection and recordId are required.");
  }
  if (!STAGES.includes(targetStage)) {
    throw new HttpsError("invalid-argument", "Invalid target dataset stage.");
  }
  if (comments.length > 4000) {
    throw new HttpsError("invalid-argument", "Comments are too long.");
  }
  if (confidenceScore !== 0 && (!Number.isInteger(confidenceScore) || confidenceScore < 1 || confidenceScore > 5)) {
    throw new HttpsError("invalid-argument", "Confidence score must be between 1 and 5.");
  }

  const role = await getTrustedRole(request.auth.uid, request.auth.token as Record<string, unknown>);
  const expertRoles = new Set(["EXPERT", "VALIDATOR", "ADMIN", "SUPER_ADMIN"]);
  if (!expertRoles.has(role)) {
    throw new HttpsError("permission-denied", "Expert verification authority is required.");
  }

  const ref = db.collection(collection).doc(recordId);
  const actorUid = request.auth.uid;

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw new HttpsError("not-found", "Dataset record not found.");

    const data = snap.data() ?? {};
    const currentStage = String(data.dataStage ?? data.stage ?? "RAW").toUpperCase() as DatasetStage;
    if (!STAGES.includes(currentStage)) {
      throw new HttpsError("failed-precondition", "Record has an invalid current dataset stage.");
    }
    if (!ALLOWED_TRANSITIONS[currentStage].includes(targetStage)) {
      throw new HttpsError("failed-precondition", `Invalid transition: ${currentStage} -> ${targetStage}.`);
    }

    if (data.ownerUid === actorUid || data.contributorId === actorUid) {
      throw new HttpsError("permission-denied", "Contributors cannot verify their own records.");
    }
    if (await hasUnresolvedModeration(recordId)) {
      throw new HttpsError("failed-precondition", "Resolve moderation reports before advancing this record.");
    }

    const status = String(data.status ?? "").toUpperCase();
    if (["EXPERT_VERIFIED", "RESEARCH_READY", "RELEASED"].includes(targetStage) && status !== "APPROVED") {
      throw new HttpsError("failed-precondition", "The record must be approved before research lifecycle advancement.");
    }

    if (targetStage === "EXPERT_VERIFIED" && !comments) {
      throw new HttpsError("invalid-argument", "Expert verification requires verification notes.");
    }
    if (targetStage === "RESEARCH_READY") {
      if (!data.license) throw new HttpsError("failed-precondition", "License metadata is required.");
      if (!data.contributorId && !data.ownerUid) throw new HttpsError("failed-precondition", "Contributor provenance is required.");
      if (confidenceScore < 3) throw new HttpsError("failed-precondition", "Research readiness requires confidence of at least 3/5.");
    }
    if (targetStage === "RELEASED" && role !== "ADMIN" && role !== "SUPER_ADMIN") {
      throw new HttpsError("permission-denied", "Only administrators can release research-ready data.");
    }

    const now = FieldValue.serverTimestamp();
    tx.update(ref, {
      dataStage: targetStage,
      stage: targetStage,
      stageChangedBy: actorUid,
      stageChangedAt: now,
      stageComments: comments,
      stageConfidenceScore: confidenceScore || null,
      updatedAt: now,
    });

    const auditRef = db.collection("auditLogs").doc();
    tx.set(auditRef, {
      action: "DATA_STAGE_TRANSITION",
      actorUid,
      collection,
      recordId,
      previousStage: currentStage,
      newStage: targetStage,
      comments,
      confidenceScore: confidenceScore || null,
      createdAt: now,
    });
  });

  return { ok: true, collection, recordId, stage: targetStage };
});

export const voteOnCommunityPost = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication is required.");

  const postId = String(request.data?.postId ?? "").trim();
  if (!postId) throw new HttpsError("invalid-argument", "postId is required.");

  const postRef = db.collection("communityPosts").doc(postId);
  const voteRef = postRef.collection("votes").doc(request.auth.uid);

  await db.runTransaction(async (tx) => {
    const post = await tx.get(postRef);
    if (!post.exists) throw new HttpsError("not-found", "Community post not found.");

    const vote = await tx.get(voteRef);
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
