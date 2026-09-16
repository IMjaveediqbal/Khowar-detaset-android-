# Contributor Collection Architecture

## Goal

Maximize authentic Khowar data collection without forcing contributors to understand internal roles or complete unnecessary fields. The platform follows:

> Collect broadly first; clean, validate, and publish systematically later.

## Account model

- Public self-registration creates a `CONTRIBUTOR` account automatically.
- Public users never select a privileged role during registration.
- `VALIDATOR`, `EXPERT`, `RESEARCHER`, `MODERATOR`, `DATA_STEWARD`, `AUDITOR`, `ADMIN`, and `SUPER_ADMIN` are assigned only through authorized administrative workflows.
- All account types use one common authentication screen. The assigned role controls the dashboard and permissions after login.

## Contribution experience

Provide two modes:

### Quick contribution

Only the essential fields are required:

- Contribution type
- Khowar content
- Consent/ownership confirmation

Optional information can be added later.

### Advanced contribution

Optional fields may include:

- Transliteration
- English meaning
- Urdu meaning
- Additional meanings
- Example sentence
- Example translations
- Dialect and region
- Part of speech
- Pronunciation notes
- Audio recordings
- Source and speaker context
- Confidence level
- Cultural explanation
- Related words and variants

## Supported contribution types

- Word
- Phrase
- Sentence
- Voice/pronunciation
- Story/folklore
- Proverb/idiom
- Cultural knowledge
- Image/visual label
- Translation
- Conversation
- Grammar example
- Place/person/traditional name

## Data quality pipeline

Contributor submissions must be retained as traceable records and move through a controlled lifecycle:

`RAW/DRAFT → SUBMITTED → UNDER_REVIEW → NEEDS_REVISION → VALIDATED → EXPERT_VERIFIED → APPROVED → PUBLISHED`

Incomplete or uncertain but potentially useful data should enter the review queue instead of being blocked unnecessarily.

## Variants and duplicates

A possible duplicate should generate a warning, not an automatic rejection. Contributors may be adding:

- Another meaning
- Another dialect
- Another pronunciation
- Another example sentence
- A homonym
- A regional variant

## Contributor permissions

Contributors may:

- Create and save drafts
- Edit their own unapproved submissions
- Add translations, examples, and audio later
- View submission status
- Suggest corrections to existing records
- Report duplicates or incorrect information

Contributors may not:

- Directly edit another user's records
- Approve or validate records
- Change roles or permissions
- Modify published records directly

## Quality ownership

- Contributor: maximum collection and contextual information
- Validator: basic review and duplicate/error detection
- Expert: linguistic and cultural verification
- Data Steward: metadata, categorization, and dataset quality
- Admin: governance, staff access, and publication control

The raw collection layer and clean verified dataset layer should remain logically separate so valuable unverified data is not lost while protecting the public/research release from unreviewed content.
