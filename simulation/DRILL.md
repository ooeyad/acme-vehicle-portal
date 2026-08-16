# The Cycle Drill — run one full month in about 40 minutes

A graded rehearsal of a complete iteration. You make the call, then check yourself.

**Cover the ANSWER blocks before reading each scenario.** Getting one wrong here costs nothing;
getting it wrong in September costs a release.

## Setup

```bash
cd acme-vehicle-portal
git checkout develop
./.claude/bin/sim-cycle timeline      # see the whole month at a glance first
```

We're rehearsing **iteration 2026.09**: development from 13 Aug, code freeze **9 Sep**,
go-live **16 Sep**. That gives a 7-day stabilization window overlapping 2026.10 development.

Two ways to play each round:

- **Fast:** `./.claude/bin/sim-cycle <date> <branch>` — preview what Claude is told. Changes nothing.
- **Real:** `export ACME_SIM_DATE=<date>` then `claude` — a live session that believes it's that
  date. Do it this way for at least rounds 3, 5 and 7.

Run `unset ACME_SIM_DATE` when you finish.

---

## Round 1 — 20 Aug · development

Issue #10 arrives: *"Add a CSV export button to the dealer inventory screen."* Product wants it in
2026.09.

**Q: Which branch do you cut, from what base?**

<details><summary>ANSWER</summary>

`feature/10-csv-export` cut from **`develop`**.

Freeze is 20 days out and this is a story, so it targets the iteration in development. Command:
`/start-work 10`.

*If you got this wrong:* you probably said `release/2026.09` — but that branch doesn't exist yet.
It's only cut at freeze. Before freeze there is nowhere else for feature work to go.
</details>

---

## Round 2 — 7 Sep · pre-freeze, 2 days out

A colleague asks you to start a refactor of `ServiceLocator` into proper dependency injection.
Realistically 5 days of work.

**Q: Do you start it? What do you tell them?**

<details><summary>ANSWER</summary>

**No.** It cannot merge and be tested before the 9 Sep freeze, so it either misses the release or —
worse — gets rushed in half-finished and destabilises it.

Say: *"That's 5 days, freeze is in 2. I'll start it on develop after the freeze so it lands in
2026.10 with a full development window."* Then create the issue with fix version 2026.10.

Check yourself: `./.claude/bin/sim-cycle 2026-09-07 develop` — Claude says the same thing.

*If you got this wrong:* the failure mode isn't missing the deadline, it's a half-merged refactor
sitting in the release you're about to freeze.
</details>

---

## Round 3 — 9 Sep · freeze day. You are the release manager.

**Q: What are the five things you do today, in order?**

<details><summary>ANSWER</summary>

`/release-manager freeze` walks it. In order:

1. **Verify the calendar first.** If the announced date moved and `.claude/release-calendar.json`
   wasn't updated, every guardrail is now wrong. Fix the file before anything else.
2. **Cut the branch:** `git checkout develop && git pull --ff-only && git checkout -b release/2026.09 && git push -u origin release/2026.09`
3. **Protect it** in GitHub: no direct pushes, PR only, build must pass.
4. **Reconcile the issues.** Everything with fix version 2026.09 is either merged, or moved to
   2026.10, or it blocks the freeze. Produce that list and say which is which.
5. **Announce:** branch name, go-live date, and that develop is now open for 2026.10.

*Most-missed:* step 1. Everyone remembers to cut the branch; almost nobody remembers the calendar
is what makes the freeze guards work for the next 7 days.
</details>

---

## Round 4 — 10 Sep · stabilization, day 1

QA finds it: on the dealer inventory screen, a dealer code with a trailing space returns zero
results. Real bug, raised as issue #11 against 2026.09.

**Q: Base branch? And what must the fix include?**

<details><summary>ANSWER</summary>

`bugfix/11-dealer-code-trim` cut from **`release/2026.09`** — not develop.

Must include a **regression test that fails without the fix**. If you can't reproduce it, stop:
a fix you can't verify is a guess, and guesses don't go into a frozen release.

Use `/fix-defect 11`, which enforces reproduce-first and narrowest-fix.

*If you said develop:* the fix would ship in **October**, not in the release the bug was raised
against. QA re-tests on the 15th, the bug is still there, and now everyone's confused.
</details>

---

## Round 5 — 10 Sep · same day, different developer

Your colleague starts issue #12, a feature for 2026.10.

**Q: Where does *their* work go? Are you both right?**

<details><summary>ANSWER</summary>

`feature/12-...` from **`develop`**. And yes — **you are both correct simultaneously.**

This is the overlap, and it's the single hardest thing about your process. On 10 Sep:
- `release/2026.09` is frozen: defect fixes only
- `develop` is wide open for 2026.10

Prove it:
```bash
./.claude/bin/sim-cycle 2026-09-10 release/2026.09
./.claude/bin/sim-cycle 2026-09-10 develop
```
Same date, opposite instructions. That's why the hook keys off *branch as well as date* — no
static document can tell a developer which set of rules applies to them today.
</details>

---

## Round 6 — 11 Sep · the tempting shortcut

Fixing #11 you notice a library upgrade would solve it more cleanly. You edit `pom.xml`.

**Q: What happens, and what should you do?**

<details><summary>ANSWER</summary>

**The hook blocks the edit.** Dependency changes on a release branch aren't defect fixes, and a
late library swap is a classic way to break a release that was already tested.

Do the narrow fix (trim the input) on the release branch. Raise a separate issue for the upgrade,
fix version 2026.10, and do it on develop with a full window.

Try it: `printf '{"tool_input":{"file_path":"pom.xml"}}' | ./.claude/hooks/scripts/guard-release-branch.sh`
while on a release branch.

*Note the shape of this:* the hook doesn't ask Claude to be disciplined. It removes the option.
</details>

---

## Round 7 — 12 Sep · #11 is merged to `release/2026.09`

**Q: What is the step everyone forgets? What happens if you skip it?**

<details><summary>ANSWER</summary>

**Back-merge to `develop`.**

```bash
git checkout develop && git merge --no-ff origin/release/2026.09 && git push
```

Skip it and the trailing-space bug **returns in 2026.10**, because develop never received the fix.
It reappears in October as a "new" bug, gets a new issue number, and nobody connects the two. You
pay for the same fix twice and look like you shipped a regression.

This is the #1 failure mode of a freeze window that overlaps the next iteration. It is why
`acme-backmerge-check` exists.
</details>

---

## Round 8 — 13 Sep · the daily check

```bash
./.claude/bin/acme-backmerge-check release/2026.09
```

**Q: It reports 2 commits not in develop. Is that a problem? What do you do?**

<details><summary>ANSWER</summary>

Yes — two fixes that will regress in October. Resolve it **today**, not on go-live day: the list
gets harder to reason about the longer it sits, and by day 5 you're merging conflicting changes
under time pressure.

`/release-manager daily` runs this plus two more checks: scope creep on the release branch (every
commit must carry a bug issue) and open defects against the fix version, with an honest read on
whether the go-live date still holds.
</details>

---

## Round 9 — 15 Sep · production is on fire

A bug in the **currently live** release (2026.08) is breaking dealer logins. Cannot wait for the
16th.

**Q: Base branch? And where does it need to end up?**

<details><summary>ANSWER</summary>

`hotfix/13-dealer-login` cut from **`main`** — not develop, not the release branch. Production is
running what's on `main`.

Then it merges to **three** places:
1. `main` (and tag it) — fixes production
2. `develop` — so 2026.10 has it
3. `release/2026.09` — because that branch is still open and would otherwise ship *without* the fix,
   overwriting the hotfix on the 16th

Check: `./.claude/bin/sim-cycle 2026-09-15 hotfix/13-dealer-login` — Claude lists all three.

*If you missed #3:* your go-live tomorrow silently reverts the emergency fix you shipped today.
This is the nastiest failure in the whole model.
</details>

---

## Round 10 — 16 Sep · go-live

**Q: What is the one condition that blocks the release outright, and what is the last step?**

<details><summary>ANSWER</summary>

**Blocker:** `acme-backmerge-check` must be clean. Drift at go-live means shipping a known future
regression. It's a blocker, not a warning.

Then: tag `v2026.09`, merge to `main`, back-merge `main` into `develop`, close the fix version,
delete the release branch.

**Last step, and the one that quietly matters most:** update `.claude/release-calendar.json` —
mark 2026.09 released and add 2026.11 with its announced dates.

*If you skip it:* nothing errors. The hooks keep running and keep giving **stale guidance** — the
worst kind of failure, because it looks like it's working. Two minutes, once a month.
</details>

---

## Scoring

| Score | Read |
|---|---|
| 9–10 | You can run this. Start with the real thing next iteration. |
| 6–8 | Re-read the rounds you missed — they're probably 5, 7 and 9, which are the ones the model exists to handle. |
| ≤5 | Run the drill again with `ACME_SIM_DATE` live sessions rather than `sim-cycle` previews. Being *in* the session teaches this faster than reading it. |

**The three that predict everything:** round 5 (both developers right at once), round 7 (back-merge),
round 9 (hotfix reaches three branches). Miss those and the model will hurt you. Get them and the
rest is bookkeeping.

```bash
unset ACME_SIM_DATE     # don't leave simulation mode on
```
