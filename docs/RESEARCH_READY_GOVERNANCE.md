# Research-Ready Governance

A contribution must pass the following gates before it is treated as research-ready:

1. **Consent** — the applicable consent record exists and is granted.
2. **Metadata** — dialect, region, source and required record metadata are complete.
3. **Quality** — normalization, duplicate checks and media-quality checks are complete where applicable.
4. **Validation** — a validator decision and confidence score are recorded.
5. **Expert verification** — a qualified expert confirms linguistic/cultural correctness for release candidates.
6. **Provenance** — contributor, source, license and timestamps remain traceable.
7. **Versioning** — the record is included in a named dataset release with reproducible version information.

## Stage policy

`RAW → QUALITY_CHECKED → COMMUNITY_VERIFIED → EXPERT_VERIFIED → RESEARCH_READY → RELEASED`

The application should never interpret a client-side button as proof that a gate passed. The authoritative release transition must be enforced by trusted backend logic and recorded in the audit trail.

## Review principles

- Prefer correctness over contribution volume.
- Preserve the original contribution; corrections should be traceable.
- Record reviewer identity, decision, comments, confidence and timestamp.
- Keep speaker/contributor privacy separate from public dataset fields.
- Do not expose research-ready data until the required consent, validation and licensing checks pass.
