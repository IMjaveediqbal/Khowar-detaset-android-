const {test,before,after}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {initializeTestEnvironment,assertFails,assertSucceeds}=require('@firebase/rules-unit-testing');
const {doc,setDoc,updateDoc,getDoc}=require('firebase/firestore');
let env,db,auth,api;
before(async()=>{
 process.env.GCLOUD_PROJECT='demo-khowar';
 api=require('./lib/index');
 db=require('firebase-admin/firestore').getFirestore();auth=require('firebase-admin/auth').getAuth();
 env=await initializeTestEnvironment({projectId:'demo-khowar',firestore:{host:'127.0.0.1',port:8080,rules:fs.readFileSync('../firestore.rules','utf8')}});
 for(const [uid,role] of [['contributor','CONTRIBUTOR'],['reader','CONTRIBUTOR'],['validator','VALIDATOR'],['expert','EXPERT'],['admin','ADMIN'],['super','SUPER_ADMIN']]){
  await auth.createUser({uid,email:uid+'@test.invalid'});await db.collection('users').doc(uid).set({role,displayName:uid});
 }
});
after(async()=>{await env?.cleanup();await require('firebase-admin/app').deleteApp(require('firebase-admin/app').getApp());});
const call=(name,uid,data)=>api[name].run({auth:{uid,token:{}},data});
const word={khowarWord:'کھوار',normalizedKhowarWord:'کھوار',englishMeaning:'Khowar',urduMeaning:'',transliteration:'',partOfSpeech:'NOUN',dialectId:'Central',regionId:'Chitral',licenseId:'CC-BY-SA-4.0'};
test('submission retries are idempotent and cannot choose lifecycle or owner',async()=>{
 const data={collection:'lexicon',recordId:'word-one',record:{...word,dataStage:'RELEASED',ownerUid:'super'},consent:true,consentVersion:'1.0'};
 await call('submitDataset','contributor',data);await call('submitDataset','contributor',data);
 const row=(await db.doc('lexicon/word-one').get()).data();assert.equal(row.ownerUid,'contributor');assert.equal(row.dataStage,'RAW');
 await assert.rejects(call('submitDataset','validator',data));
});
test('self review fails and a reviewer decision is stored with audit',async()=>{
 const data={collection:'lexicon',recordId:'self',record:word,consent:true,consentVersion:'1.0'};
 await call('submitDataset','validator',data);
 await assert.rejects(call('reviewSubmission','validator',{collection:'lexicon',recordId:'self',decision:'APPROVED',comments:'checked',confidenceScore:4}));
 await call('reviewSubmission','expert',{collection:'lexicon',recordId:'self',decision:'APPROVED',comments:'checked',confidenceScore:4});
 assert.equal((await db.doc('lexicon/self').get()).data().status,'APPROVED');
 assert.equal((await db.doc('validation_reviews/lexicon_self_expert').get()).data().validatorId,'expert');
});
test('rules prohibit client lifecycle injection and expose pending only to owner/reviewers',async()=>{
 const contributor=env.authenticatedContext('contributor').firestore();const outsider=env.authenticatedContext('outsider').firestore();const validator=env.authenticatedContext('validator').firestore();
 await assertFails(setDoc(doc(contributor,'lexicon','injected'),{...word,status:'SUBMITTED',ownerUid:'contributor',dataStage:'RELEASED'}));
 await assertFails(getDoc(doc(outsider,'lexicon','word-one')));
 await assertSucceeds(getDoc(doc(contributor,'lexicon','word-one')));
 await assertSucceeds(getDoc(doc(validator,'lexicon','word-one')));
 await assertFails(updateDoc(doc(contributor,'users','contributor'),{role:'SUPER_ADMIN'}));
});
test('community replies update counters atomically and retries do not double count',async()=>{
 await db.doc('communityPosts/post').set({ownerUid:'contributor',answerCount:0,voteScore:0});
 const data={postId:'post',commentId:'comment-one',body:'Helpful reply'};
 await call('addCommunityComment','validator',data);await call('addCommunityComment','validator',data);
 assert.equal((await db.doc('communityPosts/post').get()).data().answerCount,1);
 await assertFails(updateDoc(doc(env.authenticatedContext('validator').firestore(),'communityPosts/post/comments/comment-one'),{accepted:true}));
});
test('withdrawn records are excluded from research export',async()=>{
 await db.doc('lexicon/exportable').set({...word,id:'exportable',createdAt:Date.now(),ownerUid:'contributor',contributorId:'contributor',status:'APPROVED',dataStage:'RESEARCH_READY',consentGranted:true});
 const before=await call('listDataset','expert',{collection:'lexicon',export:true});assert.ok(before.records.some(x=>x.id==='exportable'));
 await call('withdrawConsent','contributor',{collection:'lexicon',recordId:'exportable'});
 assert.equal((await db.doc('lexicon/exportable').get()).data().status,'ARCHIVED');
 const after=await call('listDataset','expert',{collection:'lexicon',export:true});assert.ok(!after.records.some(x=>x.id==='exportable'));
});
test('admin cannot demote super admin',async()=>{
 await assert.rejects(call('setUserRole','admin',{targetUid:'super',role:'CONTRIBUTOR',reason:'Security test'}));
 assert.equal((await db.doc('users/super').get()).data().role,'SUPER_ADMIN');
});

test('withdrawal prevents queued records being uploaded later and sends public tombstones',async()=>{
 const createdAt=Date.now()-10000;
 await call('withdrawConsent','contributor',{collection:'sentences'});
 await assert.rejects(call('submitDataset','contributor',{collection:'lexicon',recordId:'late',record:{...word,createdAt},consent:true,consentVersion:'1.0'}));
 const result=await call('listDataset','validator',{collection:'lexicon'});
 assert.ok(result.records.some(x=>x.id==='exportable'&&x.status==='ARCHIVED'));
 const publicResult=await call('listDataset','reader',{collection:'lexicon'});
 assert.deepEqual(publicResult.records.find(x=>x.id==='exportable'),{id:'exportable',tombstone:true});
});
test('concurrent opposite review decisions cannot both commit',async()=>{
 await db.doc('lexicon/race').set({...word,id:'race',createdAt:Date.now(),ownerUid:'contributor',contributorId:'contributor',status:'SUBMITTED',consentGranted:true,dataStage:'RAW'});
 const data={collection:'lexicon',recordId:'race',comments:'Independent review',confidenceScore:4};
 const results=await Promise.allSettled([call('reviewSubmission','validator',{...data,decision:'APPROVED'}),call('reviewSubmission','expert',{...data,decision:'REJECTED'})]);
 assert.equal(results.filter(x=>x.status==='fulfilled').length,1);
});

test('profile updates preserve the protected server role',async()=>{
 const result=await call('saveProfile','expert',{displayName:'Expert reviewer',region:'Chitral',role:'SUPER_ADMIN'});
 assert.equal(result.role,'EXPERT');
 assert.equal((await db.doc('users/expert').get()).data().role,'EXPERT');
});
