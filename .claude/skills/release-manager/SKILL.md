---
name: release-manager
description: Release manager checklist for code freeze, the stabilization window, and go-live. Use when cutting a release branch, running the daily freeze check, or preparing a go-live.
disable-model-invocation: true
argument-hint: "[freeze|daily|golive]"
---

# Release manager: $ARGUMENTS

Our freeze window overlaps the next iteration's development, so two branches are live at once.
Almost every recurring release problem comes from that overlap. These three checklists exist to
catch it.

## `freeze` — cut the release branch on code-freeze day

1. Confirm today matches `codeFreeze` for the active iteration in `.claude/release-calendar.json`.
   If the date moved and the file was not updated, **update the file first** — every guardrail in
   the repository reads it, so a stale calendar silently disables the freeze protections.
2. Cut the branch:
   ```bash
   git fetch origin
   git checkout develop && git pull --ff-only
   git checkout -b release/<iteration> && git push -u origin release/<iteration>
   ```
3. Set the branch permission in Bitbucket: no direct pushes, PR only, and require the build to pass.
4. In Jira, confirm every ticket with this fix version is merged. Anything still open either moves
   to the next fix version or blocks the freeze. Produce the list and name which is which.
5. Announce: branch name, go-live date, and that `develop` is now open for the next iteration.

## `daily` — run every day of the stabilization window

```bash
./.claude/bin/acme-backmerge-check
```

1. **Back-merge drift.** Any commit on the release branch that is not in `develop` is a defect
   that will regress next iteration. Resolve the same day; the list gets harder to reason about
   the longer it sits.
2. **Scope creep.** Review the release branch log:
   ```bash
   git log --no-merges --oneline origin/develop..origin/release/<iteration>
   ```
   Every commit must carry a Bug ticket key. Anything else — a refactor, a dependency bump, a
   story that "was nearly done" — should not be in a frozen release. Flag it by name.
3. **Open defects.** List Bug tickets with this fix version that are not yet merged, with priority.
   State plainly whether the go-live date is still realistic.

## `golive` — the day of release

1. `./.claude/bin/acme-backmerge-check` must be clean. This is a **release blocker**, not a warning: shipping
   with drift means shipping a known future regression.
2. Merge the release branch to `main` and tag it:
   ```bash
   git checkout main && git merge --no-ff origin/release/<iteration>
   git tag -a v<iteration> -m "Release <iteration>" && git push origin main --tags
   ```
3. Back-merge `main` into `develop` one final time, so the merge commit and tag are on both.
4. Release the fix version in Jira and move any unfinished tickets to the next one.
5. Update `.claude/release-calendar.json`: mark this iteration `released`, and add the iteration
   after next with its announced dates. **This is the step that keeps every guardrail accurate for
   the next month.** It takes two minutes; skipping it is how the freeze protections quietly stop
   working.
6. Delete the release branch once `main` and `develop` both contain it.
