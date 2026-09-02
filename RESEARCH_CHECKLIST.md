# Research Release Readiness Checklist

Use this checklist before presenting a dataset release as research-ready.

## Governance
- [ ] Contributor consent recorded with current consent version
- [ ] License/usage terms recorded
- [ ] Withdrawal process documented
- [ ] Privacy review completed
- [ ] Copyright/moderation issues resolved

## Data quality
- [ ] Required metadata completeness checked
- [ ] Duplicate detection completed
- [ ] Normalization completed and documented
- [ ] Invalid/empty records removed or flagged
- [ ] AI-assisted records identified

## Speech quality
- [ ] Audio format/sample-rate requirements checked
- [ ] Duration anomalies checked
- [ ] Recording-condition metadata reviewed
- [ ] Technical quality threshold documented
- [ ] Low-quality recordings excluded or separately tagged

## Human validation
- [ ] Independent validators assigned
- [ ] Self-validation prevented
- [ ] Decision and confidence recorded
- [ ] Disagreements resolved according to protocol
- [ ] Inter-annotator agreement calculated where applicable

## Benchmark integrity
- [ ] Speaker-disjoint train/validation/test splits
- [ ] No duplicate utterances across splits
- [ ] Dialect distribution reported
- [ ] Evaluation metrics defined before testing
- [ ] Baseline model documented

## Release
- [ ] Dataset version assigned
- [ ] Changelog prepared
- [ ] Data dictionary included
- [ ] Processing methodology included
- [ ] Known limitations documented
- [ ] Integrity/checksum manifest prepared where applicable
- [ ] Release approved by authorized researcher/data steward
