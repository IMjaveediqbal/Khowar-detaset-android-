# Khowar Dataset — Research Data Dictionary

## Purpose
This document defines the minimum metadata and provenance expected for research-ready Khowar speech and language resources. It is a specification, not a claim that every existing record already satisfies every field.

## Lifecycle
`RAW` → `QUALITY_CHECKED` → `COMMUNITY_VERIFIED` → `EXPERT_VERIFIED` → `RESEARCH_READY` → `RELEASED`

A record must not be described as research-ready merely because it was submitted or approved in the application.

## Core provenance
Every research record should retain:
- Stable record ID
- Contributor/speaker pseudonymous ID
- Record type
- Creation timestamp
- Current status
- Source/provenance description
- Dataset release/version, when released
- License/usage terms
- AI assistance flag and model identifier, when applicable

## Speech metadata
Recommended minimum:
- speaker_public_id
- age_group
- gender (optional/self-described)
- native_speaker
- dialect_id
- region_id
- duration_seconds
- sample_rate
- channels
- format
- recording_environment
- transcript_khowar
- normalized_transcript
- translation fields where available
- objective quality metrics where implemented
- validation status and review history

## Linguistic metadata
For lexical and sentence resources:
- normalized form
- original form
- transliteration, if available
- English/Urdu translation where available
- part of speech / grammatical category where applicable
- dialect and region
- source type
- contributor/speaker provenance
- validation status

## Validation
A validation review should record:
- validator pseudonymous ID
- decision
- comments/reason
- confidence score
- timestamp

Self-validation must remain prohibited. Multiple independent reviews should be used when calculating inter-annotator agreement.

## Quality gates
Suggested research-release gates:
1. Consent/license verified.
2. Duplicate and normalization checks completed.
3. Required metadata present.
4. Human validation completed according to the release protocol.
5. Audio passes the defined technical quality threshold where applicable.
6. Speaker-disjoint train/validation/test assignment is preserved for speech benchmarks.
7. Known limitations and exclusions documented.

## Evaluation
Future ASR releases should report at minimum:
- Word Error Rate (WER)
- Character Error Rate (CER), where appropriate
- Number of speakers
- Hours of speech
- Utterance count
- Dialect distribution
- Recording-condition distribution
- Train/validation/test sizes
- Speaker overlap policy

Cross-lingual experiments should document source languages, transfer method, training data size, and evaluation protocol.

## Governance
Consent, withdrawal, licensing, privacy, copyright reports, moderation decisions, and material dataset changes should be auditable. Public research releases should avoid directly exposing unnecessary personal information.

## Reproducibility
Every released dataset should have:
- immutable version identifier
- release date
- data dictionary
- processing/cleaning description
- validation methodology
- license
- known limitations
- checksum or integrity manifest when distributed as files
- changelog from the previous release
