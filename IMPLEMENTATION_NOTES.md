# Implementation notes

Base commit: cc1ac69d84f6aa47036b46725918ac896044bec7.
Branch: improve/reliability-and-governance.

## Changes

- Restored the official Gradle 9.3.1 wrapper and standard debug signing; split debug/release App Check providers.
- Consolidated duplicate RBAC declarations and removed unused conflicting services. Fixed missing helper and obsolete screenshot references.
- Firebase email/password authentication, profile persistence, trusted role refresh and guarded startup without Firebase configuration.
- Room v3 migration, indexes and a durable upload queue; transactionally save record/consent/audit/upload metadata.
- Callable backend submission, ownership checks, payload validation, idempotency, role hierarchy enforcement, transactional review and exact lifecycle permissions.
- Paginated cloud record retrieval, authenticated media, withdrawal markers/tombstones, persisted review/audit/release caches.
- Fixed comment transactions, counter retries and protected accepted-answer metadata; added authorized answer acceptance and report resolution operations.
- Connected detailed review navigation and image/knowledge review records. Added real image selection, microphone permission flow, correct audio format and stable per-contributor speaker pseudonyms.
- Fixed empty-Latin-query search and allowed legitimate homonym/dialect duplicates.
- Research exports use server eligibility checks, retain all six record types, serialize structured data and can be saved to a real file. Backend draft snapshots have explicit pilot size limits.
- Removed fake API issuance, clarified heuristic suggestions, preserved form state across configuration/navigation, persisted settings and applied locale direction.
- Added backend policy and Firebase integration tests and CI gates. Backup rules exclude local corpus/credentials.

## Rollout work requiring the owner's environment

Deploy the coordinated Firebase rules/functions/indexes to a development project, configure providers/App Check, migrate legacy records with verified ownership, and run two-device and release-signing checks. Changes are submitted on a dedicated pull request branch. No Firebase deployment or merge is part of this change.

This change does not supply a trained Khowar AI, a production HTTP research API, native-speaker translation certification, a large-corpus release service or multi-speaker fieldwork management. Those features remain explicitly identified instead of being represented as working placeholders.

## Validation

Backend policy tests and Firebase emulator integration tests were run during implementation. Android compilation and final CI verification are pending; consult the pull request checks before merging.

## Concurrent main-branch changes

Integrated main through 361e577. Preserved its duplicate-RBAC removal and transliteration hint helper, corrected the helper's ASCII condition, and kept hints opt-in rather than silently inserting them into saved records. CI uses the restored Gradle 9.3.1 wrapper required by the existing Android plugin instead of main's temporary Gradle 8.13 workaround; release compilation does not require an owner's signing key.
