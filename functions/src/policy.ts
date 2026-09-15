export const roles = ["VISITOR", "CONTRIBUTOR", "VALIDATOR", "EXPERT", "RESEARCHER", "MODERATOR", "DATA_STEWARD", "AUDITOR", "ADMIN", "SUPER_ADMIN"] as const;
export type Role = typeof roles[number];
export const staffRoles: Role[] = ["VALIDATOR", "EXPERT", "RESEARCHER", "MODERATOR", "DATA_STEWARD", "AUDITOR", "ADMIN"];
export const reviewers: Role[] = ["VALIDATOR", "EXPERT", "DATA_STEWARD", "ADMIN", "SUPER_ADMIN"];
export const researchers: Role[] = ["EXPERT", "RESEARCHER", "DATA_STEWARD", "ADMIN", "SUPER_ADMIN"];
export const stages = ["RAW", "QUALITY_CHECKED", "COMMUNITY_VERIFIED", "EXPERT_VERIFIED", "RESEARCH_READY", "RELEASED"] as const;
export const rank: Record<Role, number> = { VISITOR: 0, CONTRIBUTOR: 10, VALIDATOR: 20, EXPERT: 30, RESEARCHER: 30, MODERATOR: 30, DATA_STEWARD: 40, AUDITOR: 40, ADMIN: 80, SUPER_ADMIN: 100 };
export function canChangeRole(actor: Role, previous: Role, next: Role): boolean {
  return next !== "VISITOR" && (actor === "SUPER_ADMIN" || (actor === "ADMIN" && rank[previous] < rank.ADMIN && rank[next] < rank.ADMIN));
}
export function canProvisionStaff(actor: Role, next: Role): boolean {
  return staffRoles.includes(next) && canChangeRole(actor, "CONTRIBUTOR", next);
}
export function canAdvance(role: Role, current: string, next: string): boolean {
  const index = stages.indexOf(current as typeof stages[number]);
  if (index < 0 || stages[index + 1] !== next) return false;
  if (next === "RELEASED") return ["ADMIN", "SUPER_ADMIN"].includes(role);
  if (["EXPERT_VERIFIED", "RESEARCH_READY"].includes(next)) return ["EXPERT", "DATA_STEWARD", "ADMIN", "SUPER_ADMIN"].includes(role);
  return reviewers.includes(role);
}
export const fields: Record<string, string[]> = {
  lexicon: ["khowarWord", "normalizedKhowarWord", "transliteration", "englishMeaning", "urduMeaning", "partOfSpeech", "grammaticalCategory", "definition", "pronunciation", "exampleSentenceKhowar", "exampleSentenceEnglish", "source", "isAiAssisted", "aiModelUsed"],
  sentences: ["khowarText", "normalizedText", "transliteration", "englishTranslation", "urduTranslation", "context", "source", "isAiAssisted"],
  speech: ["speakerPublicId", "speakerAgeGroup", "speakerGender", "isNativeSpeaker", "durationSeconds", "sampleRate", "channels", "format", "transcriptKhowar", "normalizedTranscript", "transliteration", "englishTranslation", "urduTranslation", "recordingEnvironment", "mediaPath"],
  stories: ["title", "khowarText", "transliteration", "englishTranslation", "urduTranslation", "category", "authorOrSpeaker", "source"],
  knowledge: ["type", "title", "khowarContent", "transliteration", "englishContent", "urduContent", "explanation", "source"],
  images: ["title", "description", "khowarLabel", "englishLabel", "culturalContext", "photographerOrSource", "mediaPath"]
};
export function validatePayload(collection: string, data: Record<string, unknown>): string | null {
  if (!fields[collection]) return "Unknown collection";
  const numeric = new Set(['durationSeconds','sampleRate','channels']);
  const boolean = new Set(['isAiAssisted','isNativeSpeaker']);
  for (const key of fields[collection]) {
    const value=data[key];
    if(value === undefined || value === null) continue;
    if(numeric.has(key)) { if(typeof value !== 'number' || !Number.isFinite(value))return `${key} must be finite`; }
    else if(boolean.has(key)) { if(typeof value !== 'boolean')return `${key} must be boolean`; }
    else if(typeof value !== 'string')return `${key} must be text`;
  }
  const textField: Record<string, string> = { lexicon: "khowarWord", sentences: "khowarText", speech: "transcriptKhowar", stories: "khowarText", knowledge: "khowarContent", images: "khowarLabel" };
  const text = data[textField[collection]];
  if (typeof text !== "string" || !text.trim() || text.length > (collection === "lexicon" ? 500 : 100000)) return "Invalid Khowar text";
  if (["lexicon", "sentences"].includes(collection)) {
    const en = data[collection === "lexicon" ? "englishMeaning" : "englishTranslation"];
    const ur = data[collection === "lexicon" ? "urduMeaning" : "urduTranslation"];
    if (![en, ur].some(x => typeof x === "string" && x.trim())) return "A translation is required";
  }
  if (!['CC-BY-SA-4.0', 'CC-BY-4.0', 'CC-0', 'RESEARCH_ONLY'].includes(String(data.licenseId))) return "Invalid license";
  for (const key of ['dialectId', 'regionId']) if (typeof data[key] !== 'string' || !(data[key] as string).trim()) return `${key} required`;
  if (collection === "speech" && (typeof data.durationSeconds !== "number" || !Number.isFinite(data.durationSeconds) || data.durationSeconds <= 0 || data.durationSeconds > 3600)) return "Invalid audio duration";
  if (Object.values(data).some(v => typeof v === 'string' && v.length > 100000)) return "Text too long";
  return null;
}