import { HttpsError, onCall } from "firebase-functions/v2/https";
import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { roles, Role, canChangeRole } from "./policy";

if (!getApps().length) initializeApp();
const auth = getAuth();
const db = getFirestore();
const options = { enforceAppCheck: true, maxInstances: 10 };

export const provisionManagedAccount = onCall(options, async request => {
  const actorUid = request.auth?.uid;
  if (!actorUid) throw new HttpsError("unauthenticated", "Sign in first.");
  const actorAccount = await auth.getUser(actorUid);
  if (actorAccount.disabled) throw new HttpsError("permission-denied", "Account disabled.");
  const actorProfile = await db.collection("users").doc(actorUid).get();
  const actorRole = (actorProfile.data()?.role ?? actorAccount.customClaims?.role ?? "CONTRIBUTOR") as Role;
  if (actorRole !== "ADMIN" && actorRole !== "SUPER_ADMIN") throw new HttpsError("permission-denied", "Only an administrator can provision project accounts.");

  const email = String(request.data?.email ?? "").trim().toLowerCase();
  const password = String(request.data?.temporaryPassword ?? "");
  const displayName = String(request.data?.displayName ?? "").trim();
  const region = String(request.data?.region ?? "").trim();
  const requestedRole = String(request.data?.role ?? "") as Role;
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new HttpsError("invalid-argument", "A valid email address is required.");
  if (password.length < 12 || password.length > 128) throw new HttpsError("invalid-argument", "Temporary password must be 12–128 characters.");
  if (displayName.length < 2 || displayName.length > 100 || region.length > 100) throw new HttpsError("invalid-argument", "Valid display name and region are required.");
  if (!roles.includes(requestedRole) || requestedRole === "VISITOR" || requestedRole === "CONTRIBUTOR") throw new HttpsError("invalid-argument", "Choose a managed project role.");
  if (!canChangeRole(actorRole, "CONTRIBUTOR", requestedRole)) throw new HttpsError("permission-denied", "You cannot provision this role.");

  let createdUid = "";
  try {
    const account = await auth.createUser({ email, password, displayName, emailVerified: false, disabled: false });
    createdUid = account.uid;
    const now = Date.now();
    await auth.setCustomUserClaims(createdUid, { role: requestedRole, managedAccount: true, mustChangePassword: true });
    await db.collection("users").doc(createdUid).set({ id: createdUid, email, displayName, username: "", region, role: requestedRole, managedAccount: true, mustChangePassword: true, provisionedBy: actorUid, provisionedAt: now, createdAt: now, isPublicProfile: false }, { merge: true });
    await db.collection("auditLogs").doc().set({ action: "MANAGED_ACCOUNT_PROVISIONED", actorUid, targetUid: createdUid, targetEmail: email, role: requestedRole, createdAt: now });
    return { ok: true, uid: createdUid, email, role: requestedRole, mustChangePassword: true };
  } catch (error) {
    if (createdUid) { try { await auth.deleteUser(createdUid); } catch { /* preserve original failure */ } }
    const code = (error as { code?: string })?.code;
    if (code === "auth/email-already-exists") throw new HttpsError("already-exists", "An account with this email already exists.");
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Managed account could not be created.");
  }
});

export const completeManagedPasswordChange = onCall(options, async request => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in first.");
  const account = await auth.getUser(uid);
  if (account.disabled) throw new HttpsError("permission-denied", "Account disabled.");
  const profileRef = db.collection("users").doc(uid);
  const profile = await profileRef.get();
  if (profile.data()?.managedAccount !== true || profile.data()?.mustChangePassword !== true) return { ok: true, required: false };

  const provisionedAt = Number(profile.data()?.provisionedAt ?? 0);
  const passwordUpdatedAt = Number(account.passwordUpdatedAt ?? 0);
  if (!provisionedAt || !passwordUpdatedAt || passwordUpdatedAt <= provisionedAt) {
    throw new HttpsError("failed-precondition", "Change the temporary password before completing account setup.");
  }

  const now = Date.now();
  await db.runTransaction(async tx => {
    const current = await tx.get(profileRef);
    if (current.data()?.mustChangePassword !== true) return;
    tx.set(profileRef, { mustChangePassword: false, passwordChangedAt: now }, { merge: true });
    tx.set(db.collection("auditLogs").doc(), { action: "MANAGED_ACCOUNT_PASSWORD_CHANGED", actorUid: uid, targetUid: uid, createdAt: now });
  });
  await auth.setCustomUserClaims(uid, { ...account.customClaims, managedAccount: true, mustChangePassword: false });
  return { ok: true, required: false };
});
