import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { roles, Role } from "./policy";

if (!getApps().length) initializeApp();
const auth = getAuth();
const db = getFirestore();
const options = { enforceAppCheck: true, maxInstances: 10 };

export const saveProfileDetails = onCall(options, async request => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in first.");

  const account = await auth.getUser(uid);
  if (account.disabled || !account.email) throw new HttpsError("permission-denied", "Use an active email/password account.");

  const displayName = String(request.data?.displayName ?? "").trim();
  const username = String(request.data?.username ?? "").trim();
  const region = String(request.data?.region ?? "").trim();
  const bio = String(request.data?.bio ?? "").trim();

  if (displayName.length < 2 || displayName.length > 100) {
    throw new HttpsError("invalid-argument", "Display name must be 2–100 characters.");
  }
  if (!/^[A-Za-z0-9_.-]{2,32}$/.test(username)) {
    throw new HttpsError("invalid-argument", "Username must use 2–32 letters, numbers, _, -, or .");
  }
  if (region.length < 1 || region.length > 100) {
    throw new HttpsError("invalid-argument", "Region or community must be 1–100 characters.");
  }
  if (bio.length > 300) {
    throw new HttpsError("invalid-argument", "About you must be 300 characters or fewer.");
  }

  const ref = db.collection("users").doc(uid);
  const result = await db.runTransaction(async tx => {
    const existing = await tx.get(ref);
    const data = existing.data() ?? {};
    const claimedRole = account.customClaims?.role;
    const role: Role = (typeof data.role === "string" && roles.includes(data.role as Role))
      ? data.role as Role
      : (typeof claimedRole === "string" && roles.includes(claimedRole as Role) ? claimedRole as Role : "CONTRIBUTOR");
    const now = Date.now();
    const profile = {
      id: uid,
      email: account.email,
      displayName,
      username,
      region,
      bio,
      role,
      updatedAt: now
    };
    tx.set(ref, profile, { merge: true });
    return profile;
  });

  return result;
});
