---
name: quick-fix
description: The fast lane for genuinely trivial changes - a typo, a message-bundle string, a comment, a log level. Skips plan mode, reproduce-first and the review subagents. Refuses automatically if the change turns out not to be trivial.
disable-model-invocation: true
argument-hint: "[one-line description of the fix]"
allowed-tools: Bash(git status *) Bash(git diff *) Bash(git checkout -b *) Bash(git add *) Bash(git commit *) Bash(git push *) Bash(./.claude/bin/quick-fix-eligible) Bash(mvn *) Bash(gh pr create *)
---

# Quick fix: $ARGUMENTS

The fast lane exists so that trivial changes don't carry the full process. It skips plan mode,
the reproduce-first test discipline, and both review subagents.

That's only safe when the change is **small AND away from anything shared**. You don't get to
decide that by feel — `quick-fix-eligible` decides it from the actual diff, and it refuses more
often than people expect.

## 1. Sanity check before touching anything

State in one line what you're about to change and which file. If the answer involves more than
two files, or any of these, **stop now and tell me to use `/start-work` instead**:

- shared or base classes, DAOs, or any SQL
- `web.xml`, `struts-config.xml`, `pom.xml`, migrations, pipeline or infra files
- anything under `.claude/`
- anything where "what else calls this?" is a real question

Save yourself the round trip: if you're unsure, it isn't a quick fix.

## 2. Branch

```bash
git checkout develop
git pull --ff-only
git checkout -b chore/<short-slug>
```

No issue is required for the fast lane. The branch name and commit message carry the description
instead, so make them specific — `chore/vin-error-wording`, not `chore/fix`.

## 3. Make the change

Just make it. No plan mode, no failing test first.

Do **not** widen the change while you're in there. Noticing a second thing to fix is the single
most common way a quick fix stops being one — raise an issue for it instead.

## 4. Prove it's still a quick fix

```bash
./.claude/bin/quick-fix-eligible
```

**If it refuses**, stop. Do not commit. Tell me what it said and follow its escalation steps:
create an issue, rename the branch to `feature/<n>-<slug>` or `bugfix/<n>-<slug>`, and switch to
the full process. Nothing is lost — the work stays on the branch.

**If it passes**, run the build:

```bash
mvn -pl legacy-web -am test
```

Green, or it isn't a quick fix any more.

## 5. Commit and open the PR

```bash
git add <the specific files>
git commit -m "chore: <what changed and why, one line>"
git push -u origin HEAD
gh pr create --base develop --title "chore: <summary>" --body "Fast-lane change. <What and why.>

Eligibility: <paste the quick-fix-eligible output>
Tests: <paste the test summary line>

No issue - fast lane per the two-lane rule in CLAUDE.md."
```

Pasting the eligibility output into the PR body matters: it's what tells the reviewer this
skipped the review subagents deliberately rather than by accident, and it's how you'd audit
fast-lane usage later.

## When this is the wrong tool

Anything with a behaviour change. Anything a user could notice going wrong. Anything during a
freeze window — the checker refuses on `release/*` outright, because stabilization needs a bug
issue, a regression test and a back-merge regardless of how small the fix looks.

If you find yourself reaching for `/quick-fix` more than a couple of times a week, the boundary
is probably drawn wrong. Tell the platform owner — that's data, not a workaround.
