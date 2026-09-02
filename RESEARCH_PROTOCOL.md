# Khowar Dataset — Research & Data Governance Protocol

## Purpose

This protocol defines how community-contributed Khowar linguistic resources should move from collection to research-ready release. It is intended to support collaboration with universities and researchers working on low-resource NLP, speech technology, ASR and cross-lingual modelling.

## 1. Data lifecycle

`RAW → QUALITY CHECK → COMMUNITY VERIFIED → EXPERT VERIFIED → RESEARCH READY → VERSIONED RELEASE`

Raw contributions must remain traceable. Cleaning or normalization must not destroy the original contribution.

## 2. Speaker and dialect coverage

Speech resources should retain an anonymous/public speaker identifier and, where voluntarily provided and ethically appropriate, age group, gender, native-speaker status, region and dialect. Public displays should avoid unnecessary personal information.

Researchers should report speaker counts and coverage by dialect/region rather than only total recordings or hours.

## 3. Speech quality

Each speech record should retain duration, sample rate, channels, format, recording environment and a quality/reviewer score. Rejected or low-quality recordings should remain auditable but should not enter a research release unless explicitly justified.

## 4. Validation

Validation reviews should retain validator identity, decision, comments, confidence score and timestamp. Self-validation must be prohibited.

When multiple independent validators assess the same item, the release process should calculate inter-annotator agreement where statistically appropriate. Agreement metrics should be reported separately from simple approval rate.

## 5. Consent and licensing

Contributions intended for publication or research use must have an explicit, versioned consent record associated with the contribution. Consent withdrawal must be auditable.

Every release must state its license, attribution requirements and any restrictions. Do not assume that a default license is legally appropriate for every contribution; review the final governance and consent language with the collaborating institution.

## 6. Dataset releases

Every research release should have:

- A unique version number
- Release name and date
- Record and speaker counts
- Speech hours
- Dialect/region coverage
- Validation statistics
- Quality criteria
- Train/dev/test split policy
- License and consent statement
- Known limitations
- Preprocessing description
- Change log from the previous release

## 7. ASR/NLP benchmark principles

For speech modelling, evaluation splits should be speaker-disjoint to prevent the same speaker appearing in training and evaluation sets. Report both word error rate (WER) and character error rate (CER) where suitable.

For low-resource experiments, report performance at multiple training-data sizes so that data-efficiency gains can be measured. Cross-lingual experiments should document the source languages, transfer method and preprocessing.

## 8. Reproducibility

A research result should identify the exact dataset release, preprocessing version, split manifest, model configuration and evaluation procedure used. Never overwrite a published dataset release with later corrections; publish a new version instead.

## 9. Privacy and security

Do not expose email addresses, authentication identifiers, raw API secrets or unnecessary personal metadata in public dataset exports. API tokens must be stored as hashes and shown only once. Access to restricted resources should be authenticated and auditable.

## 10. Research collaboration

The platform is a data-collection and governance foundation, not a claim that the corpus is already scientifically validated. Research collaborators should jointly define annotation guidelines, sampling strategy, quality thresholds, benchmark tasks and publication/data-sharing policy before declaring a corpus research-ready.
