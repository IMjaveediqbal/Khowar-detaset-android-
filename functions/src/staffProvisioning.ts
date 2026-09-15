import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { Role, canProvisionStaff } from "./policy";

const auth = getAuth();
const db = getFirestore();
const options = { enforceAppCheck: true, maxInstances: 10 };

function uidOf(request: { auth?: { uid: string } }): string {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in first.");
  return request.auth.uid;
}

async function roleOf(uid: string): Promise<Role> {
  const account = await auth.getUser(uid);
  if (account.disabled) throw new HttpsError("permission-denied", "Account disabled.");
  const profile = await db.collection("users").doc(uid).get();
  const role = String(profile.data()?.role ?? account.customClaims?.role ?? "CONTRIBUTOR") as Role;
  return ["VISITOR", "CONTRIBUTOR", "VALIDATOR", "EXPERT", "RESEARCHER", "MODERATOR", "DATA_STEWARD", "AUDITOR", "ADMIN", "SUPER_ADMIN"].includes(role)
    ? role
    : "CONTRIBUTOR";
}

/**
 * Creates a staff account server-side. No password is accepted or generated here.
 * The client subsequently requests Firebase's password-reset email for the target email.
 */
export const provisionStaffAccount = onCall(options, async request => {
  const actorUid = uidOf(request);
  const actorRole = await roleOf(actorUid);
  const email = String(request.data?.email ?? "").trim().toLowerCase();
  const displayName = String(request.data?.displayName ?? "").trim();
  const nextRole = String(request.data?.role ?? "").toUpperCase() as Role;
  const reason = String(request.data?.reason ?? "Staff account provisioned by authorized administrator.").trim();

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    throw new HttpsError("invalid-argument", "Enter a valid staff email address.");
  }
  if (displayName.length < 2 || displayName.length > 100) {
    throw new HttpsError("invalid-argument", "Enter a name between 2 and 100 characters.");
  }
  if (reason.length < 5 || reason.length > 1000) {
    throw new HttpsError("invalid-argument", "A valid provisioning reason is required.");
  }
  if (!canProvisionStaff(actorRole, nextRole)) {
    throw new HttpsError("permission-denied", "Your role cannot provision this staff role.");
  }

  let existing;
  try {
    existing = await auth.getUserByEmail(email);
  } catch (error) {
    const code = (error as { code?: string }).code;
    if (code !== "auth/user-not-found") throw error;
  }
  if (existing) {
    throw new HttpsError("already-exists", "An account already exists for this email. Use role management instead.");
  }

  const created = await auth.createUser({ email, displayName });
  const now = Date.now();

  try {
    await auth.setCustomUserClaims(created.uid, { role: nextRole });
    await db.runTransaction(async tx => {
      const userRef = db.collection("users").doc(created.uid);
      tx.create(userRef, {
        id: created.uid,
        email,
        displayName,
        username: "",
        region: "",
        role: nextRole,
        accountType: "STAFF",
        provisionedBy: actorUid,
        provisionedAt: now,
        roleUpdatedBy: actorUid,
        roleUpdatedAt: now,
      });
      tx.create(db.collection("auditLogs").doc(), {
        action: "STAFF_ACCOUNT_PROVISIONED",
        actorUid,
        targetUid: created.uid,
        targetEmail: email,
        newRole: nextRole,
        reason,
        createdAt: now,
      });
    });
  } catch (error) {
    await auth.deleteUser(created.uid).catch(() => undefined);
    throw error;
  }

  return { ok: true, uid: created.uid, email, role: nextRole };
});
