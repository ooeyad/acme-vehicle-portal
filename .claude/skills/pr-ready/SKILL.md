---
name: pr-ready
description: Run the ACME pre-PR checklist - build, test, review, commit with the Jira key, and open the Bitbucket pull request against the right target branch. Use when ready to open a PR or to commit and push a finished change.
disable-model-invocation: true
argument-hint: "[optional VP-123 if not in the branch name]"
allowed-tools: Bash(git status *) Bash(git diff *) Bash(git log *) Bash(git add *) Bash(git commit *) Bash(git push *) Bash(acme-ticket *) Bash(acme-pr *) Bash(./.claude/bin/acme-backmerge-check *)
---

# Pre-PR checklist

Work through these in order. Stop and report if any step fails — do not proceed past a failure.

## 1. Establish the ticket and the target

```bash
git rev-parse --abbrev-ref HEAD
gh issue view "$(git rev-parse --abbrev-ref HEAD | grep -oE '[0-9]+' | head -1)"
```

The ticket key comes from the branch name (or `$ARGUMENTS` if the branch predates the convention).
If there is no key in either, stop: every change traces to a ticket.

The PR target follows the branching model, and `acme-pr` defaults to it:

| Source branch | Target |
|---|---|
| `feature/*` | `develop` |
| `bugfix/*` cut from a release branch | that `release/*` branch |
| `bugfix/*` cut from develop | `develop` |
| `hotfix/*` | `main` |

If you are on a `bugfix/*` branch, confirm which base it was actually cut from with
`git merge-base --fork-point`. Targeting `develop` with a fix meant for the release is how a
defect misses the go-live it was raised for.

## 2. Show me what changed

`git status` and `git diff` against the target branch. Summarise in two sentences. If the diff
contains changes unrelated to the ticket, say so and stop.

## 3. Build and test

Run this repository's build and test commands (they are in CLAUDE.md). Paste the actual output,
not a summary of it.

## 4. Review

Use the `code-reviewer` subagent on the diff. If the change touches authentication, database
access, file upload, or customer/VIN data, also use `security-reviewer`. On the Struts stack, run
`db2-sql-reviewer` over any new or changed SQL.

Fix **Critical** findings and re-run the tests. Leave Warnings and Notes for the human reviewer.

## 5. Commit

One commit per logical change. The Jira key goes first so Bitbucket links the commit to the ticket:

```
VP-123 <imperative one-line summary>

<why the change was needed, not what the diff shows>

VP-123 #comment Fixed by <what you did>. Verified with <test command>.
```

Two rules about smart commits, and they matter:

- Use `#comment` only.
- **Never** use a transition command such as `#done`, `#resolve` or `#close`, and never `#time`.
  A ticket moved to Done by an agent has not been tested by anyone. Humans transition tickets.

## 6. Open the PR

```bash
git push -u origin HEAD
gh pr create --base develop --title "VP-123: <summary>" "<body>"
```

The PR body must contain: what changed, why, how it was verified (paste the test command and
result), and the blast radius.

## 7. If this was a release-branch fix, flag the back-merge

Run `./.claude/bin/acme-backmerge-check` and report the result. A fix that lands on a release branch and never
reaches `develop` regresses in the next iteration — that is our most common recurring defect, and
this is the moment to catch it.

---

Never push directly to `main`, `master`, `develop`, or a `release/*` branch.
