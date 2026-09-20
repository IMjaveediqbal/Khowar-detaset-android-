const test = require('node:test');
const assert = require('node:assert/strict');
const {canChangeRole,canAdvance,validatePayload}=require('./lib/policy');
test('administrators can appoint admins but never assign or modify super admins',()=>{
 assert.equal(canChangeRole('ADMIN','SUPER_ADMIN','CONTRIBUTOR'),false);
 assert.equal(canChangeRole('ADMIN','CONTRIBUTOR','ADMIN'),true);
 assert.equal(canChangeRole('ADMIN','ADMIN','CONTRIBUTOR'),false);
 assert.equal(canChangeRole('ADMIN','CONTRIBUTOR','EXPERT'),true);
 assert.equal(canChangeRole('ADMIN','CONTRIBUTOR','SUPER_ADMIN'),false);
 assert.equal(canChangeRole('SUPER_ADMIN','CONTRIBUTOR','SUPER_ADMIN'),false);
 assert.equal(canChangeRole('CONTRIBUTOR','CONTRIBUTOR','ADMIN'),false);
});
test('expert verification requires expert authority and consecutive stage',()=>{
 assert.equal(canAdvance('VALIDATOR','COMMUNITY_VERIFIED','EXPERT_VERIFIED'),false);
 assert.equal(canAdvance('EXPERT','COMMUNITY_VERIFIED','EXPERT_VERIFIED'),true);
 assert.equal(canAdvance('EXPERT','RAW','RESEARCH_READY'),false);
 assert.equal(canAdvance('EXPERT','RESEARCH_READY','RELEASED'),false);
 assert.equal(canAdvance('ADMIN','RESEARCH_READY','RELEASED'),true);
});
test('submission validation rejects missing source text and invalid durations',()=>{
 const base={khowarWord:'کھوار',englishMeaning:'',urduMeaning:'کھوار',dialectId:'Central',regionId:'Chitral',licenseId:'CC-BY-SA-4.0'};
 assert.equal(validatePayload('lexicon',base),null);
 assert.ok(validatePayload('lexicon',{...base,khowarWord:''}));
 for(const durationSeconds of [NaN,Infinity,0,-1,3601])assert.ok(validatePayload('speech',{...base,transcriptKhowar:'کھوار',mediaPath:'speech/user/recording',durationSeconds}));
 assert.equal(validatePayload('speech',{...base,transcriptKhowar:'کھوار',mediaPath:'speech/user/recording',durationSeconds:15}),null);
});
