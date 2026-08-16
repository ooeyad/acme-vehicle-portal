---
name: start-work
description: Start work on a Jira ticket - pick the correct base branch for where we are in the release cycle, create the branch, and load the ticket. Use when beginning a new ticket, story, or bug.
disable-model-invocation: true
argument-hint: "[VP-123]"
allowed-tools: Bash(git status *) Bash(git fetch *) Bash(git checkout *) Bash(git pull *) Bash(git branch *) Bash(acme-ticket *)
---

# Start work on $ARGUMENTS

Getting the base branch wrong is the most expensive mistake in our process: work based on the
wrong branch either misses the release or drags unreleased changes into it. Decide deliberately.

## 1. Load the ticket

```
gh issue view $ARGUMENTS
```

Read the **type** and the **fix version**. They determine everything below. If the fix version is
empty, stop and ask which iteration this is for — do not guess.

## 2. Choose the base branch

Read the release-cycle line injected at the start of this session, then apply this table:

| Ticket type | Fix version | Base branch | New branch |
|---|---|---|---|
| Story / Task / Improvement | the iteration currently in development | `develop` | `feature/$ARGUMENTS-<slug>` |
| Bug found in the release under test | the iteration currently in stabilization | the active `release/*` branch | `bugfix/$ARGUMENTS-<slug>` |
| Bug in production, cannot wait | the next iteration | `main` | `hotfix/$ARGUMENTS-<slug>` |
| Bug found in development, not yet released | the iteration in development | `develop` | `bugfix/$ARGUMENTS-<slug>` |

If the ticket is a Story and the fix version is the iteration currently in stabilization, that is a
contradiction: a story cannot enter a frozen release. Say so and stop.

## 3. Create the branch

```bash
git fetch origin
git checkout <base>
git pull --ff-only origin <base>
git checkout -b <type>/$ARGUMENTS-<short-slug>
```

The slug is three or four lowercase words, hyphen separated, describing the change — not the
ticket title verbatim. The ticket key must appear exactly as Jira spells it: our tooling reads the
branch name to derive the commit prefix, the PR title, and the Jira link.

## 4. Confirm before writing code

State back to me, in three lines:

- the ticket type, fix version, and which iteration it ships in
- the base branch you used and why
- whether we are inside a freeze window, and what that restricts

Then wait. Do not start editing until I confirm.
