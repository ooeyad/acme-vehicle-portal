---
name: legacy-safe-change
description: The protocol for changing legacy Struts/DB2 code safely. Use before editing anything under the legacy web module, or when the user asks to modify, refactor, or fix code in the old application.
paths:
  - "**/legacy-web/**"
  - "**/*Action.java"
  - "**/struts-config*.xml"
  - "**/*.jsp"
---

# Changing legacy code at ACME

This code is old, widely called, and thinly tested. Assume every method has a caller you have not
seen and a behaviour someone depends on.

## The protocol — follow it in order

1. **Map before you touch.** Use the `struts-explorer` subagent to find every caller, including
   JSP and string-based references. Do not start editing on the strength of a single grep.
2. **Pin current behaviour.** If the code you are changing has no test, write a characterization
   test first (`/characterization-test`). A refactor without a pinning test is a
   rewrite with extra steps.
3. **Prefer additive change.** Add a new method or a new action path; leave the old one delegating
   to it. Do not widen or narrow an existing public signature — something you cannot see is calling it.
4. **One concern per change.** Do not reformat, do not "tidy while you're in there", do not upgrade
   a library. Formatting churn in this codebase makes review impossible and hides the real diff.
5. **Keep the existing style.** Match the surrounding code even where it is not how you would write it
   today. Consistency beats correctness of style here.
6. **State the blast radius** before you finish: which entry points, which tables, which other modules.

## Hard rules
- **Never** edit generated code, `WebContent/vendor/**`, or anything under `target/`.
- **Never** change `struts-config.xml` forward names that JSPs reference without updating every JSP.
- **Never** put business logic in an Action. Actions parse the request, call a `*Service`, pick a forward.
- **Never** open a DB connection in an Action or a JSP.
- Schema changes are a migration file for human review — never ad-hoc DDL.

## When you cannot map it
If the callers genuinely cannot be determined — reflection, a config table, an external system —
say so and stop. Ask for a human. An unmapped change to this application is how a dealer portal
goes down on a Monday.
