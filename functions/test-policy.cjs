const test = require('node:test');
const assert = require('node:assert/strict');
const {canChangeRole,canProvisionStaff,canAdvance,validatePayload}=require('./lib/policy');
test('administrators cannot demote superior or peer accounts',()=>{
 assert.equal(canChangeRole('ADMIN','SUPER_ADMIN','CONTRIBUTOR'),false);
 assert.equal(canChangeRole('ADMIN','ADMIN','CONTRIBUTOR'),false);
 assert.equal(canChangeRole('ADMIN','CONTRIBUTOR','EXPERT'),true);
 assert.equal(canChangeRole('CONTRIBUTOR','CONTRIBUTOR','ADMIN'),false);
});
test('staff provisioning is admin-controlled',()=>{
 for(const role of ['VALIDATOR','EXPERT','RESEARCHER','MODERATOR','DATA_STEWARD','AUDITOR']) assert.equal(canProvisionStaff('ADMIN',role),true);
 assert.equal(canProvisionStaff('ADMIN','ADMIN'),false);
 assert.equal(canProvisionStaff('ADMIN','SUPER_ADMIN'),false);
 assert.equal(canProvisionStaff('SUPER_ADMIN','ADMIN'),true);
 assert.equal(canProvisionStaff('SUPER_ADMIN','SUPER_ADMIN'),false);
 assert.equal(canProvisionStaff('CONTRIBUTOR','EXPERT'),false);
});
test('expert verification requires expert authority and consecutive stage',()=>{
 assert.equal(canAdvance('VALIDATOR','COMMUNITY_VERIFIED','EXPERT_VERIFIED'),false);
 assert.equal(canAdvance('EXPERT','COMMUNITY_VERIFIED','EXPERT_VERIFIED'),true);
 assert.equal(canAdvance('EXPERT','RAW','RESEARCH_READY'),false);
 assert.equal(canAdvance('EXPERT','RESEARCH_READY','RELEASED'),false);
 assert.equal(canAdvance('ADMIN','RESEARCH_READY','RELEASED'),true);
});
test('submission validation rejects missing translation and invalid durations',()=>{
 const base={khowarWord:'کھوار',englishMeaning:'',urduMeaning:'کھوار',dialectId:'Central',regionId:'Chitral',licenseId:'CC-BY-SA-4.0'};
 assert.equal(validatePayload('lexicon',base),null);
 assert.ok(validatePayload('lexicon',{...base,urduMeaning:''}));
 for(const durationSeconds of [NaN,Infinity,0,-1,3601])assert.ok(validatePayload('speech',{...base,transcriptKhowar:'کھوار',durationSeconds}));
 assert.equal(validatePayload('speech',{...base,transcriptKhowar:'کھوار',durationSeconds:15}),null);
});
