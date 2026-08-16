---
name: spec-first
description: Interview the developer about a feature and write a spec to SPEC.md before any code is written. Use at the start of a multi-file feature, a migration, or any work where the approach is not yet obvious.
disable-model-invocation: true
argument-hint: "[one-line feature description]"
---

# Spec first

The feature is: **$ARGUMENTS**

Interview me in detail using the AskUserQuestion tool before writing anything. Ask about:

- **Scope**: what is explicitly out of scope for this change?
- **Which stack**: legacy Struts/DB2, the Spring Boot service, the React front end, or more than one?
  If more than one, what is the contract between them?
- **Data**: which tables, which columns, is a schema change needed? (Schema changes are a separate,
  human-reviewed migration — flag it if one is implied.)
- **Existing patterns**: which existing feature is the closest analogue I should copy?
- **Edge cases** I probably have not considered.
- **Verification**: what test or observable behaviour proves this works end to end?
- **Rollout**: feature flag, or straight to release?

Do not ask obvious questions. Dig into the parts that are genuinely ambiguous.

When we have covered everything, write `SPEC.md` at the repository root containing:

1. Goal, in one paragraph.
2. Explicitly out of scope.
3. Files and interfaces that will change, named.
4. Data / schema impact.
5. Edge cases and how each is handled.
6. **An end-to-end verification step** that proves the feature works.

Then stop. Do not implement. Tell me to start a fresh session with `/clear` and implement from SPEC.md —
a clean context focused entirely on implementation produces better code than continuing this one.
