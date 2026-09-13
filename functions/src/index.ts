import { onCall, HttpsError } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, FieldPath, Filter } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { createHash } from "node:crypto";
import { roles, Role, reviewers, researchers, canChangeRole, canAdvance, fields, validatePayload } from "./policy";
initializeApp();
const db = getFirestore();
const auth = getAuth();
const options = { enforceAppCheck: true, maxInstances: 10 };
const collections = Object.keys(fields);
const uidOf = (request: { auth?: { uid: string } }) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in first.");
  return request.auth.uid;
};
// Read current server-owned role, rather than stale claims after demotion.
async function roleOf(uid: string): Promise<Role> {
  const user = await auth.getUser(uid);
  if (user.disabled) throw new HttpsError("permission-denied", "Account disabled.");
  const doc = await db.collection("users").doc(uid).get();
  const role = doc.data()?.role ?? user.customClaims?.role ?? "CONTRIBUTOR";
  return roles.includes(role) ? role : "CONTRIBUTOR";
}
async function requireRole(uid: string, allowed: Role[]) {
  const role = await roleOf(uid);
  if (!allowed.includes(role)) throw new HttpsError("permission-denied", "Your role cannot perform this operation.");
  return role;
}
function recordRef(data: any) {
  const collection = String(data?.collection ?? "");
  const id = String(data?.recordId ?? "");
  if (!collections.includes(collection) || !/^[A-Za-z0-9_-]{1,128}$/.test(id)) throw new HttpsError("invalid-argument", "Invalid record reference.");
  return db.collection(collection).doc(id);
}
export const getMyRbac = onCall(options, async request => {
  const uid = uidOf(request);
  return { uid, role: await roleOf(uid) };
});
export const saveProfile = onCall(options, async request => {
  const uid = uidOf(request);
  const account = await auth.getUser(uid);
  const displayName = String(request.data?.displayName ?? "").trim();
  const region = String(request.data?.region ?? "").trim();
  if (displayName.length < 2 || displayName.length > 100 || region.length > 100) throw new HttpsError("invalid-argument", "Enter a name of 2–100 characters and a valid region.");
  if (account.disabled || !account.email) throw new HttpsError("permission-denied", "Use an active email/password account.");
  const profile = { id: uid, email: account.email, displayName, username: String(request.data?.username ?? "").slice(0,100), region, updatedAt: Date.now() };
  return db.runTransaction(async tx => {
    const ref = db.collection("users").doc(uid);
    const existing = await tx.get(ref);
    const claimedRole = account.customClaims?.role;
    const role: Role = existing.data()?.role ?? (roles.includes(claimedRole) ? claimedRole : "CONTRIBUTOR");
    // Do not write a stale role back over a concurrent administrator decision.
    tx.set(ref, existing.exists ? profile : { ...profile, role }, { merge: true });
    return { ...profile, role };
  });
});
export const setUserRole = onCall(options, async request => {
  const uid = uidOf(request);
  const actor = await requireRole(uid, ["ADMIN", "SUPER_ADMIN"]);
  const next = String(request.data?.role ?? "") as Role;
  const reason = String(request.data?.reason ?? "").trim();
  if (!roles.includes(next) || reason.length < 5 || reason.length > 1000) throw new HttpsError("invalid-argument", "Valid role and reason required.");
  const target = request.data?.targetUid ? await auth.getUser(String(request.data.targetUid)) : await auth.getUserByEmail(String(request.data?.targetEmail ?? ""));
  if (target.uid === uid) throw new HttpsError("permission-denied", "Cannot change your own role.");
  const previous = await roleOf(target.uid);
  if (!canChangeRole(actor, previous, next)) throw new HttpsError("permission-denied", "Cannot modify this account or assign this role.");
  await db.runTransaction(async tx => {
    const ref = db.collection("users").doc(target.uid);
    const snap = await tx.get(ref);
    if (!canChangeRole(actor, (snap.data()?.role ?? previous) as Role, next)) throw new HttpsError("permission-denied", "Target role changed; refresh first.");
    tx.set(ref, { role: next, roleUpdatedBy: uid, roleUpdatedAt: Date.now() }, { merge: true });
    tx.set(db.collection("auditLogs").doc(), { action: "ROLE_CHANGED", actorUid: uid, targetUid: target.uid, previousRole: previous, newRole: next, reason, createdAt: Date.now() });
  });
  // Rules also consult the protected user document, so a claim update failure cannot retain revoked privileges.
  await auth.setCustomUserClaims(target.uid, { ...target.customClaims, role: next });
  return { ok: true, role: next };
});
export const submitDataset = onCall(options, async request => {
  const uid = uidOf(request);
  const ref = recordRef(request.data);
  const input = request.data?.record as Record<string, unknown>;
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw new HttpsError('invalid-argument', 'Record required.');
  const error = validatePayload(ref.parent.id, input);
  if (error) throw new HttpsError('invalid-argument', error);
  if (request.data?.consent !== true || request.data?.consentVersion !== '1.0') throw new HttpsError('invalid-argument', 'Versioned consent required.');
  const selected: Record<string, unknown> = {};
  for (const key of [...fields[ref.parent.id], 'dialectId', 'regionId', 'licenseId']) if (input[key] !== undefined) selected[key] = input[key];
  const fingerprint = createHash('sha256').update(JSON.stringify(selected)).digest('hex');
  const existing = await ref.get();
  if (existing.exists) {
    if (existing.data()?.ownerUid !== uid || existing.data()?.fingerprint !== fingerprint) throw new HttpsError('already-exists', 'Record ID already used.');
    return { ok: true, id: ref.id };
  }
  if (['speech','images'].includes(ref.parent.id)) {
    const path = String(input.mediaPath ?? '');
    if (path !== `${ref.parent.id}/${uid}/${ref.id}`) throw new HttpsError('invalid-argument', 'Invalid media ownership.');
    const [metadata] = await getStorage().bucket().file(path).getMetadata();
    const prefix = ref.parent.id === 'speech' ? 'audio/' : 'image/';
    if (!metadata.contentType?.startsWith(prefix)) throw new HttpsError('invalid-argument', 'Invalid media type.');
  }
  const profile = await db.collection('users').doc(uid).get();
  await db.runTransaction(async tx => {
    const snap = await tx.get(ref);
    const owner = await tx.get(db.collection("users").doc(uid));
    if (Number(owner.data()?.withdrawBefore ?? 0) >= Number(input.createdAt ?? Date.now())) throw new HttpsError("failed-precondition", "Consent for this queued record was withdrawn.");
    if (snap.exists) {
      if (snap.data()?.ownerUid !== uid || snap.data()?.fingerprint !== fingerprint) throw new HttpsError('already-exists', 'Record ID already used.');
      return;
    }
    const now = Date.now();
    tx.create(ref, { ...selected, id: ref.id, ownerUid: uid, contributorId: uid, contributorName: profile.data()?.displayName ?? 'Contributor', status: 'SUBMITTED', dataStage: 'RAW', consentGranted: true, consentVersion: '1.0', createdAt: now, updatedAt: now, fingerprint });
    tx.set(db.collection('consents').doc(`${ref.parent.id}_${ref.id}`), { ownerUid: uid, collection: ref.parent.id, recordId: ref.id, granted: true, licenseId: input.licenseId, version: '1.0', grantedAt: now });
  });
  return { ok: true, id: ref.id };
});
export const listDataset = onCall(options, async request => {
  const uid = uidOf(request);
  const collection = String(request.data?.collection ?? '');
  if (!collections.includes(collection)) throw new HttpsError('invalid-argument', 'Invalid collection.');
  const role = await roleOf(uid);
  let query = db.collection(collection).orderBy(FieldPath.documentId()).limit(100);
  if (request.data?.export === true) {
    if (!researchers.includes(role)) throw new HttpsError('permission-denied', 'Research access required.');
    query = query.where('status','==','APPROVED').where('consentGranted','==',true).where('dataStage','in',['RESEARCH_READY','RELEASED']);
  } else if (!reviewers.includes(role)) {
    query = query.where(Filter.or(Filter.where('status','==','APPROVED'), Filter.where('ownerUid','==',uid), Filter.where('wasPublic','==',true)));
  }
  if (request.data?.cursor) query = query.startAfter(String(request.data.cursor));
  const snap = await query.get();
  const records = snap.docs.filter(doc => doc.data().createdAt != null).map(doc => {
    const { fingerprint, ...record } = doc.data();
    if(record.status !== 'APPROVED' && record.ownerUid !== uid && !reviewers.includes(role)) return {id:doc.id,tombstone:true};
    return record;
  });
  return { records, cursor: snap.size === 100 ? snap.docs[snap.size-1].id : null };
});
export const reviewSubmission = onCall(options, async request => {
  const uid = uidOf(request);
  await requireRole(uid, reviewers);
  const ref = recordRef(request.data);
  const decision = String(request.data?.decision ?? '');
  const comments = String(request.data?.comments ?? '').trim();
  const confidence = Number(request.data?.confidenceScore);
  if (!['APPROVED','REJECTED','CHANGES_REQUESTED'].includes(decision) || !Number.isInteger(confidence) || confidence < 1 || confidence > 5 || comments.length > 4000) throw new HttpsError('invalid-argument','Invalid review.');
  await db.runTransaction(async tx => {
    const snap = await tx.get(ref); const data = snap.data();
    if (!data || !['SUBMITTED','UNDER_REVIEW'].includes(data.status)) throw new HttpsError('failed-precondition','Record is not awaiting review.');
    if (data.ownerUid === uid || data.contributorId === uid) throw new HttpsError('permission-denied','Self-review prohibited.');
    if (!data.consentGranted) throw new HttpsError('failed-precondition','Consent withdrawn.');
    const now = Date.now();
    tx.update(ref, { status: decision, wasPublic: decision === "APPROVED" || data.wasPublic === true, reviewedBy: uid, updatedAt: now, publishedAt: decision === 'APPROVED' ? now : null });
    tx.set(db.collection('validation_reviews').doc(`${ref.parent.id}_${ref.id}_${uid}`), { collection: ref.parent.id, recordId: ref.id, validatorId: uid, decision, comments, confidenceScore: confidence, createdAt: now });
    tx.set(db.collection('auditLogs').doc(), { action:'REVIEW', actorUid:uid, collection:ref.parent.id, recordId:ref.id, decision, createdAt:now });
  });
  return { ok: true };
});
export const transitionDataStage = onCall(options, async request => {
  const uid = uidOf(request); const role = await roleOf(uid); const ref = recordRef(request.data);
  const next = String(request.data?.targetStage ?? '');
  const comments = String(request.data?.comments ?? '').trim();
  const confidence = Number(request.data?.confidenceScore ?? 0);
  if (comments.length > 4000 || !Number.isInteger(confidence) || confidence < 0 || confidence > 5) throw new HttpsError('invalid-argument','Invalid notes or confidence.');
  await db.runTransaction(async tx => {
    const snap = await tx.get(ref); const data = snap.data();
    if (!data || !canAdvance(role, data.dataStage, next)) throw new HttpsError('permission-denied','Stage transition not allowed.');
    if (data.ownerUid === uid || data.contributorId === uid) throw new HttpsError('permission-denied','Self-verification prohibited.');
    if (data.status !== 'APPROVED' || !data.consentGranted || data.moderationOpen) throw new HttpsError('failed-precondition','Approval, consent and resolved reports are required.');
    if (['EXPERT_VERIFIED','RESEARCH_READY'].includes(next) && (!comments || confidence < 3)) throw new HttpsError('invalid-argument','Verification notes and confidence of at least 3 required.');
    tx.update(ref, { dataStage:next, stageChangedBy:uid, stageConfidenceScore:confidence, stageComments:comments, updatedAt:Date.now() });
    tx.set(db.collection('auditLogs').doc(), { action:'DATA_STAGE_TRANSITION', actorUid:uid, collection:ref.parent.id, recordId:ref.id, previousStage:data.dataStage, newStage:next, comments, confidence, createdAt:Date.now() });
  });
  return { ok:true };
});
// Bounded, resumable withdrawal. Retry until remaining=false; each page is atomic.
export const withdrawConsent = onCall(options, async request => {
  const uid = uidOf(request);
  const collection = String(request.data?.collection ?? '');
  if (!collections.includes(collection)) throw new HttpsError('invalid-argument','Invalid collection.');
  if (!request.data?.recordId) await db.collection('users').doc(uid).set({withdrawBefore:Date.now()},{merge:true});
  else {
    const ref = recordRef({collection, recordId:request.data.recordId});
    await db.runTransaction(async tx => {
      const current=await tx.get(ref);
      if(current.exists && current.data()?.ownerUid!==uid)throw new HttpsError('permission-denied','Ownership mismatch.');
      if(!current.exists)tx.create(ref,{id:ref.id,ownerUid:uid,contributorId:uid,status:'ARCHIVED',consentGranted:false,withdrawnAt:Date.now()});
    });
  }
  let query = db.collection(collection).where('ownerUid','==',uid).where('consentGranted','==',true).limit(100);
  if (request.data?.recordId) query = query.where(FieldPath.documentId(),'==',String(request.data.recordId));
  const docs = await query.get();
  for (const doc of docs.docs) {
    await db.runTransaction(async tx => {
      const current = await tx.get(doc.ref);
      if (current.data()?.ownerUid !== uid) throw new HttpsError('permission-denied','Ownership mismatch.');
      tx.update(doc.ref,{consentGranted:false,status:'ARCHIVED',wasPublic:current.data()?.status==='APPROVED'||current.data()?.wasPublic===true,updatedAt:Date.now()});
      tx.set(db.collection('consents').doc(`${collection}_${doc.id}`),{granted:false,withdrawnAt:Date.now()},{merge:true});
      tx.set(db.collection('auditLogs').doc(),{action:'WITHDRAW_CONSENT',actorUid:uid,collection,recordId:doc.id,createdAt:Date.now()});
    });
  }
  return {ok:true,remaining:docs.size === 100};
});
export const reportDataset = onCall(options, async request => {
  const uid=uidOf(request); const ref=recordRef(request.data);
  const reason=String(request.data?.description??'').trim();
  if(reason.length<5||reason.length>4000) throw new HttpsError('invalid-argument','Report requires 5–4000 characters.');
  await db.runTransaction(async tx=>{const doc=await tx.get(ref);if(!doc.exists)throw new HttpsError('not-found','Record not found.');tx.update(ref,{moderationOpen:true,updatedAt:Date.now()});tx.set(db.collection('moderationReports').doc(),{reporterId:uid,collection:ref.parent.id,recordId:ref.id,reason,status:'PENDING',createdAt:Date.now()});});
  return {ok:true};
});
export const addCommunityComment = onCall(options, async request => {
  const uid=uidOf(request); const postId=String(request.data?.postId??''); const body=String(request.data?.body??'').trim(); const commentId=String(request.data?.commentId??'');
  if(!/^[\w-]{1,128}$/.test(postId)||!/^[\w-]{1,128}$/.test(commentId)||body.length<2||body.length>3000)throw new HttpsError('invalid-argument','Invalid comment.');
  const post=db.collection('communityPosts').doc(postId); const ref=post.collection('comments').doc(commentId);
  const profile=await db.collection('users').doc(uid).get();
  await db.runTransaction(async tx=>{const p=await tx.get(post);const old=await tx.get(ref);if(!p.exists)throw new HttpsError('not-found','Post not found.');if(old.exists){if(old.data()?.ownerUid!==uid||old.data()?.body!==body)throw new HttpsError('already-exists','Comment ID used.');return;}tx.create(ref,{ownerUid:uid,authorProfileId:uid,authorName:profile.data()?.displayName??'Contributor',body,accepted:false,createdAt:Date.now()});tx.update(post,{answerCount:Number(p.data()?.answerCount??0)+1,updatedAt:Date.now()});});
  return {id:ref.id};
});
export const voteOnCommunityPost = onCall(options, async request => {
  const uid=uidOf(request);const postId=String(request.data?.postId??'');if(!/^[\w-]{1,128}$/.test(postId))throw new HttpsError('invalid-argument','Invalid post.');
  const post=db.collection('communityPosts').doc(postId);const vote=post.collection('votes').doc(uid);
  await db.runTransaction(async tx=>{const p=await tx.get(post);const v=await tx.get(vote);if(!p.exists)throw new HttpsError('not-found','Post not found.');if(v.exists)tx.delete(vote);else tx.create(vote,{createdAt:Date.now()});tx.update(post,{voteScore:Math.max(0,Number(p.data()?.voteScore??0)+(v.exists?-1:1)),updatedAt:Date.now()});});return {ok:true};
});

export const createDatasetDraft = onCall(options, async request => {
  const uid=uidOf(request);await requireRole(uid,['ADMIN','SUPER_ADMIN']);
  const version=String(request.data?.version??'').trim();const name=String(request.data?.name??'').trim();
  if(!/^[A-Za-z0-9._-]{1,64}$/.test(version)||!name||name.length>160)throw new HttpsError('invalid-argument','Valid version and name required.');
  const ref=db.collection('dataset_versions').doc(version);
  // Immutable pilot snapshots: explicit size limit, never silently truncate.
  await db.runTransaction(async tx=>{
    const old=await tx.get(ref);if(old.exists)throw new HttpsError('already-exists','Version already exists.');
    const records: Record<string,unknown>[]=[];
    for(const collection of collections){
      const result=await tx.get(db.collection(collection).where('status','==','APPROVED').where('consentGranted','==',true).where('dataStage','in',['RESEARCH_READY','RELEASED']).limit(101));
      if(result.size>100)throw new HttpsError('resource-exhausted','Pilot snapshot limit is 100 records per collection. Use a background release job for larger releases.');
      for(const doc of result.docs){const {fingerprint,...record}=doc.data();records.push({collection,...record});}
    }
    if(records.length===0)throw new HttpsError('failed-precondition','No research-ready records.');
    const snapshot=JSON.stringify(records);
    if(Buffer.byteLength(snapshot)>700000)throw new HttpsError('resource-exhausted','Snapshot too large for pilot release. Use a background release job.');
    const checksum=createHash('sha256').update(snapshot).digest('hex');
    tx.create(ref,{versionNumber:version,releaseName:name,description:String(request.data?.description??'').slice(0,4000),status:'DRAFT',schemaVersion:'1.0',recordCount:records.length,recordsJson:snapshot,sha256:checksum,createdBy:uid,createdAt:Date.now()});
    tx.set(db.collection('auditLogs').doc(),{action:'CREATE_DATASET_DRAFT',actorUid:uid,version,sha256:checksum,createdAt:Date.now()});
  });return {ok:true,version,status:'DRAFT'};
});
export const acceptCommunityAnswer = onCall(options,async request=>{
  const uid=uidOf(request);const postId=String(request.data?.postId??'');const commentId=String(request.data?.commentId??'');
  if(!/^[\w-]{1,128}$/.test(postId)||!/^[\w-]{1,128}$/.test(commentId))throw new HttpsError('invalid-argument','Invalid answer.');
  const post=db.collection('communityPosts').doc(postId);const answer=post.collection('comments').doc(commentId);
  await db.runTransaction(async tx=>{
    const p=await tx.get(post);const a=await tx.get(answer);
    if(p.data()?.ownerUid!==uid)throw new HttpsError('permission-denied','Only the post author can accept an answer.');
    if(!a.exists)throw new HttpsError('not-found','Answer not found.');
    const previous=p.data()?.acceptedCommentId;
    if(previous&&previous!==commentId)tx.update(post.collection('comments').doc(previous),{accepted:false});
    tx.update(answer,{accepted:true});tx.update(post,{solved:true,acceptedCommentId:commentId,updatedAt:Date.now()});
  });return {ok:true};
});
export const resolveDatasetReports = onCall(options,async request=>{
  const uid=uidOf(request);await requireRole(uid,['MODERATOR','ADMIN','SUPER_ADMIN']);const ref=recordRef(request.data);const notes=String(request.data?.notes??'').trim();
  if(notes.length<5||notes.length>4000)throw new HttpsError('invalid-argument','Resolution notes required.');
  await db.runTransaction(async tx=>{
    const record=await tx.get(ref);if(!record.exists)throw new HttpsError('not-found','Record not found.');
    const reports=await tx.get(db.collection('moderationReports').where('collection','==',ref.parent.id).where('recordId','==',ref.id).where('status','==','PENDING').limit(400));
    if(reports.size===400)throw new HttpsError('resource-exhausted','Too many reports; process in an administrative batch.');
    reports.docs.forEach(report=>tx.update(report.ref,{status:'RESOLVED',notes,resolvedBy:uid,resolvedAt:Date.now()}));
    tx.update(ref,{moderationOpen:false,updatedAt:Date.now()});
    tx.set(db.collection('auditLogs').doc(),{action:'RESOLVE_REPORTS',actorUid:uid,collection:ref.parent.id,recordId:ref.id,notes,createdAt:Date.now()});
  });return {ok:true};
});
