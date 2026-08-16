---
name: fix-defect
description: Fix a defect on a release branch during the code-freeze window, with the narrow-change discipline stabilization requires. Use when fixing a bug found in testing after code freeze.
disable-model-invocation: true
argument-hint: "[VP-123]"
---

# Freeze-window defect fix: $ARGUMENTS

We are between code freeze and go-live. This release is being tested by people right now, and
every change carries more risk than it would during development. The goal is the **smallest
change that fixes the defect**, not the best change.

## 1. Confirm the ground

```
gh issue view $ARGUMENTS
git rev-parse --abbrev-ref HEAD
```

Check all three, and stop if any fails:

- The ticket type is **Bug**. A story does not enter a frozen release.
- The fix version matches the release branch you are on.
- You are on `bugfix/$ARGUMENTS-<slug>`, branched from the release branch — not from `develop`.

## 2. Reproduce before you fix

Write a failing test that reproduces the defect. Run it, show me it fails. If you cannot reproduce
it, say so and stop — a fix you cannot verify is a guess, and guesses do not go into a frozen release.

For legacy code with no coverage, use `/characterization-test` to pin the
surrounding behaviour first, so the fix cannot silently change something else.

## 3. Make the narrowest fix

Allowed: the defect itself, plus the test that proves it.

Not allowed in this window, even when tempting:

- Refactoring, renaming, or reformatting anything
- Dependency, build, or pipeline changes
- Fixing a *different* bug you noticed — raise a ticket instead
- Widening the fix "while we're in here"

If the correct fix genuinely requires one of these, that is a signal it should not ship in this
release. Say so explicitly and recommend deferring to the next iteration.

## 4. Verify

- The new test passes.
- The full module test suite passes. Paste the output.
- State the blast radius: what else calls this code, and why those callers are unaffected.

## 5. Land it — and back-merge

```bash
gh pr create --base develop --title "$ARGUMENTS: <one-line summary>" "<what broke, why, how verified>"
```

Then say this to me explicitly, because it is the step everyone forgets:

> This fix is on the release branch only. Once merged it must be back-merged to `develop`,
> or the defect returns in the next iteration.

Run `./.claude/bin/acme-backmerge-check` to see the current state of that drift.
