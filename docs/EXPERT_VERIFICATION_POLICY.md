# Expert Verification Policy

## Purpose

Expert verification is a second-level linguistic and cultural review after community validation. It is a governance decision, not simply another UI status.

## Required reviewer authority

The current application roles do not define a dedicated `EXPERT` role. Until that role is introduced and authenticated, expert verification must be restricted to explicitly authorized `ADMIN`/`SUPER_ADMIN` accounts. A future `EXPERT` role should be granted independently of contribution ownership.

## Required checks

Before moving a record to `EXPERT_VERIFIED`, the reviewer should confirm:

- Khowar text/audio is linguistically plausible.
- Translation meaning and context are faithful.
- Dialect/region metadata is credible.
- Consent and license allow the intended research use.
- Duplicate/conflicting records have been considered.
- Contributor and provenance information are retained.
- Previous validator decision and confidence are present.
- No unresolved moderation issue blocks the record.

## Research-ready transition

A record may move from `EXPERT_VERIFIED` to `RESEARCH_READY` only when every release gate passes. The transition must be performed by trusted backend code and written to the audit trail. The Android client must never be the authority for this transition.

## Conflict of interest

A reviewer must not approve their own contribution. Where practical, a second independent reviewer should be required for high-risk cultural, consent, or disputed records.
