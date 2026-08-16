# Claude Code Playbook — ACME

Struts/DB2 + Spring Boot/React/Azure · GitHub · monthly iterations with an overlapping freeze window

---

# Part 0 — Read this first: there are two different jobs here

The earlier version of this document tangled these together, which is why "implement the playbook"
was unclear. They are separate jobs, with separate audiences, done at separate times.

| | **Job A — The Process** | **Job B — The Rollout** |
|---|---|---|
| **What it is** | How a developer works day to day | How you get 40 people onto Job A |
| **Who does it** | Every developer, every day | You, the platform owner |
| **Where it happens** | Inside a repo | Mostly *outside* any repo — laptops, admin console, a plugin repo |
| **How long** | Forever, once running | About 8 weeks, then maintenance |
| **Covered in** | **Part 1** | **Part 2** |

**You cannot do Job B until you can do Job A yourself.** Spend a week as a developer first. That's
what `START-HERE.md` and `simulation/DRILL.md` are for.

### Where things live

| I want to... | Go to |
|---|---|
| Start using Claude Code on the repo today | `START-HERE.md` |
| Test whether I understand the release cycle | `simulation/DRILL.md` |
| Know what developers should do daily | Part 1 |
| Roll this out to the team | Part 2 |
| Know why a decision was made | Part 3 |

---

# Part 1 — The Process

What "working" looks like once it's running. Nothing here is about rollout.

## 1.1 Three roles

| Role | Who | Does |
|---|---|---|
| **Developer** | everyone | `/start-work` → code → `/pr-ready`. Follows the cycle phase they're told. |
| **Release manager** | one person per iteration | Cuts the branch at freeze, runs the daily drift check, runs go-live, **updates the calendar** |
| **Platform owner** | you | Owns `.claude/`, the plugins, managed settings. Not in the daily loop. |

## 1.2 Branching

```
main         ────●───────────────────────●──────────  tags: v2026.08, v2026.09
                  ╲                     ╱
release/2026.09    ●──●──●──●──────────●              freeze → go-live (up to 7 days)
                  ╱   ╲  ╲  ╲         ╱
develop      ──●─●─────●──●──●───────●────●──●──────  next iteration, never stops
                        ╲       ╲
feature/12               ●───────●
```

The diagonal lines back into `develop` are the **back-merges**. They are the whole game.

| Branch | From | To | Who |
|---|---|---|---|
| `feature/12-slug` | `develop` | `develop` | anyone |
| `bugfix/12-slug` | active `release/*` | that release **and** back-merge to `develop` | anyone |
| `hotfix/12-slug` | `main` | `main` + `develop` + open `release/*` | senior + RM |
| `release/<iter>` | `develop` at freeze | `main` at go-live | release manager |

## 1.3 The four phases

Phase depends on **date AND branch** — during the overlap, two phases are live at once.

| Phase | When | On which branch | Allowed |
|---|---|---|---|
| **Development** | iteration start → freeze−4d | `develop`, `feature/*` | anything |
| **Pre-freeze** | freeze−3d → freeze | `develop` | only what can land and be tested before freeze |
| **Stabilization** | freeze → go-live | `release/*` | defect fixes only: bug issue + regression test + back-merge |
| *(simultaneously)* | freeze → go-live | `develop` | full development for the **next** iteration |
| **Go-live** | go-live day | `main` | RM only: drift clean → tag → merge → update calendar |

Claude is told which phase applies at session start. You don't have to remember.

## 1.4 Developer daily loop

```
claude                 # read the RELEASE CYCLE line. It tells you what's allowed today.
/start-work 12         # picks the base branch from issue type + cycle phase
                       # Shift+Tab → plan mode for anything touching 2+ files
                       # code, run tests
/pr-ready              # build, test, review subagents, commit, PR
```

`/clear` between unrelated tasks. Every message re-sends the whole conversation.

## 1.5 Release manager loop

```
/release-manager freeze     # freeze day: verify calendar, cut branch, protect, reconcile, announce
/release-manager daily      # EVERY day of stabilization: drift, scope creep, open defects
/release-manager golive     # drift must be clean → tag → merge → back-merge → UPDATE CALENDAR
```

## 1.6 The five rules that matter most

1. **Read the cycle line before you do anything.** It changes what's allowed.
2. **A fix on a release branch must be back-merged to develop**, or it regresses next month.
3. **A hotfix goes to three places**: `main`, `develop`, and any open release branch.
4. **During stabilization: smallest possible fix, plus a regression test.** Nothing else.
5. **Update the calendar at go-live.** Skip it and every guardrail silently gives stale advice.

## 1.7 Rehearse before you rely on it

```bash
./.claude/bin/sim-cycle timeline              # the whole month in one screen
./.claude/bin/sim-cycle 2026-09-10 develop    # any moment, changes nothing
export ACME_SIM_DATE=2026-09-10 && claude     # a live session as if it were that date
```

Then work through `simulation/DRILL.md` — ten graded scenarios covering a full month.
**Do this before Part 2.** If you can't pass the drill you can't teach it to 40 people.

---

# Part 2 — The Rollout

Seven phases. Each has a **goal**, **steps**, **done when**, and **what breaks if you skip it**.
Nothing here happens inside a project repo except Phase 3.

> **Timing rule:** never start a phase during a stabilization window. Changing how people work while
> they're stabilising a release is how a tool gets blamed for a missed go-live.

| # | Phase | Who | Roughly |
|---|---|---|---|
| 0 | Learn it yourself | you | Week 0 |
| 1 | Baseline & pilot | you + 5 devs | Week 1 |
| 2 | Guardrails | you + security | Week 1–2 |
| 3 | Per-repo config | repo owners | Week 2–3 |
| 4 | Package & distribute | you | Week 3–4 |
| 5 | Skills from evidence | stack leads | Week 4–5 |
| 6 | Cost discipline | everyone | Week 5–6 |
| 7 | Measure & govern | you | Week 7–8+ |

---

### Phase 0 — Learn it yourself

**Goal:** you can run the process without reading anything.

1. `START-HERE.md`, four exercises.
2. `simulation/DRILL.md`, all ten rounds. Score 9+.
3. Make one real change end to end with `/start-work` → `/pr-ready`.
4. Change something in `.claude/` because it annoyed you. This is the important one.

**Done when:** you've modified the config based on your own friction, not my guesses.

**Skip it and:** you'll roll out a standard you've never used, and the first hard question from a
senior developer will have no answer.

---

### Phase 1 — Baseline & pilot

**Goal:** know what "before" looks like, and find your real requirements.

1. Confirm the facts: GitHub Cloud or Enterprise Server; auth path (Teams/Enterprise seats vs
   Console vs Azure/Foundry — note Foundry credentials alone don't unlock Code Review, Routines,
   web sessions or Remote Control); **who is on Eclipse vs IntelliJ**.
2. Get the next three iterations' announced freeze and go-live dates. Fill in the calendar.
3. Pick 4–6 pilots, at least 2 from the legacy Struts team. Include one skeptic.
4. Turn on the analytics dashboard (~24h to populate).
5. Record the baseline: PRs/dev/week, defects found per iteration, defects escaping to production,
   actual vs planned freeze duration.
6. Pilots use Claude Code normally for a week, then run `/insights`. **Give them no standards yet.**

**Done when:** you have 6 `/insights` reports and a baseline number.

**Skip it and:** you'll write skills for problems nobody has, and you'll never prove ROI because
there's no "before".

> Budget: ~$13/dev/active day, $150–250/dev/month, 90% under $30/active day.

---

### Phase 2 — Guardrails

**Goal:** nothing dangerous is possible, before anyone gets clever.

1. Deploy `templates/managed-settings.json` — server-managed via the admin console, or the
   per-OS file path, plus `HKLM` on Windows. Developers cannot override it.
2. **Decide the Windows question.** The OS-level sandbox runs on macOS/Linux/WSL2, *not* native
   Windows. Standardise on WSL2, use a dev container, or accept permission-rules-only isolation on
   Windows and tighten the deny rules to compensate.
3. Lock the extension surface: `strictKnownMarketplaces` (use a `hostPattern`, not a literal URL —
   exact matching doesn't normalise `.git` suffixes or `ssh://`), `disableSideloadFlags`,
   `allowManagedMcpServersOnly`.

**Done when:** a developer runs `/status` and sees `Setting sources: Enterprise managed settings`.

**Skip it and:** the first incident is a credential in a commit or a query against production DB2,
and the programme ends there.

---

### Phase 3 — Per-repo config

**Goal:** every repo carries its own context. *This is the phase that fixes "everyone works
differently."*

1. `/init` (set `CLAUDE_CODE_NEW_INIT=1` for the interactive version), then `/doctor` to trim.
2. **Hard ceiling: 200 lines.** Past that, adherence measurably drops — Claude starts ignoring
   rules because the important ones are buried. If someone's sessions are inconsistent, check
   their CLAUDE.md length first.
3. Move file-type rules into `.claude/rules/` with `paths:` frontmatter so they load only when
   relevant.
4. Add `Read` deny rules for checked-in generated and vendored code.
5. Add the release calendar and the cycle hooks.
6. Install code-intelligence plugins on the typed stack.

**Done when:** every active repo has a committed CLAUDE.md under 200 lines, `/context` confirms it
loads, and the cycle line appears at session start.

**Skip it and:** you've bought a licence, not a standard.

---

### Phase 4 — Package & distribute

**Goal:** one commit gives a new developer everything.

1. Convert `.claude/` into plugins; push `claude-standards` to your GitHub org.
2. Add `extraKnownMarketplaces` + `enabledPlugins` to each repo's committed `.claude/settings.json`.
3. **Don't set `version` in `plugin.json`** for internal plugins — without it Claude Code uses the
   commit SHA and people get updates when you push. Set `"1.0.0"` and forget to bump it and
   everyone silently keeps a stale copy.

**Done when:** a teammate clones a repo, trusts the folder, and has the whole standard with zero
manual setup.

**Skip it and:** six repos drift into six dialects within a quarter.

---

### Phase 5 — Skills from evidence

**Goal:** capture what people actually repeat.

Go through the Phase 1 `/insights` reports. Ship **4–6 skills per stack, not 30**. Keep bodies
short — an invoked skill stays in context all session. Use `disable-model-invocation: true` for
anything with side effects; it also drops context cost to zero until invoked.

**Done when:** each skill has been used by two people who didn't write it.

**Skip it and:** you write 30 skills from imagination, descriptions get truncated because there
are too many, and Claude picks the wrong ones.

---

### Phase 6 — Cost discipline

Ranked by impact:

1. **`/clear` between unrelated tasks** — the single biggest lever
2. **Sonnet as the enforced default**, Opus opt-in
3. **Delegate verbose work to subagents** — test runs, log analysis, "find every caller"
4. **CLAUDE.md under 200 lines**, reference material in skills
5. **Filter noisy tool output with a `PreToolUse` hook** — the Maven filter turns a 40,000-token
   reactor log into a few hundred
6. **Lower effort for mechanical work** (`/effort low`) — thinking tokens bill as output
7. Code intelligence · `Read` deny rules · CLI over MCP · plan mode · specific prompts

**Done when:** `/usage` on the pilot squad no longer flags long context as >10% of usage.

---

### Phase 7 — Measure & govern

**Standard metrics:** adoption (watch for **dips** — friction, not disinterest), PRs with Claude
Code %, accept rate, spend/user. Attribution is deliberately conservative and *underestimates*
impact — use it for trends, not absolute claims.

**Cycle metrics that will matter more to your management:**

- defects found during stabilization (should fall — review subagents catch more pre-merge)
- **back-merge drift incidents (should reach zero)**
- freeze window actual vs planned duration (should stabilise)
- defects escaping to production per iteration

| Artifact | Owner | Review |
|---|---|---|
| `managed-settings.json` | you + Security | change request |
| `release-calendar.json` | release manager | monthly, at go-live |
| core plugin | you | PR, 2 approvals |
| stack plugins | stack tech lead | PR, 1 approval |
| repo `CLAUDE.md` / rules | repo owners | normal PR review |

**Quarterly:** re-run `/doctor`; delete rules written around older model limitations; retire unused
skills (`OTEL_LOG_TOOL_DETAILS=1` makes `skill_activated` record names).

---

# Part 3 — Reference

## 3.1 Decisions and why

| # | Decision | Why | Revisit if |
|---|---|---|---|
| D1 | Gitflow, not trunk-based | Your freeze window runs two workstreams at once; trunk-based needs feature flags on everything, unrealistic for 2009 Struts | You already have a model the team follows |
| D2 | Release calendar as a committed file | Your freeze dates move monthly; anything hardcoded is wrong by definition | Never — this is load-bearing |
| D3 | Phase from date **and branch** | On 10 Sep a dev on `release/*` and one on `develop` need opposite advice | Never |
| D4 | Freeze window enforced by hook | CLAUDE.md is advisory; "usually follows" isn't good enough mid-freeze | You want humans-only enforcement |
| D5 | Back-merge drift blocks go-live | The #1 failure of an overlapping freeze window | Never |
| D6 | Branch name is the contract | One convention drives five integrations | Never |
| D7 | Claude never closes an issue | An issue closed by an agent hasn't been tested by anyone | Never |
| D8 | CLI over MCP for tickets | `gh` costs zero context; MCP costs a tool listing every session | You move to Jira Cloud and want writes — then use the Atlassian Rovo MCP (Cloud only; no official DC support) |
| D11 | IntelliJ plugin; **Eclipse uses the terminal** | No Eclipse integration exists. Your legacy devs are most likely on Eclipse and have the most to gain — say this out loud or you lose them | Never |
| D12 | Sonnet enforced default | "Opus left as default" is one of the two habits behind nearly every cost outlier | Never |

## 3.2 What each config file does

| File | Loads | Purpose |
|---|---|---|
| `CLAUDE.md` | every session, in full | Always-on rules. **Under 200 lines.** |
| `.claude/rules/*.md` | when a matching file is touched | Per-file-type conventions |
| `.claude/skills/*/SKILL.md` | description always, body when invoked | Workflows and reference |
| `.claude/agents/*.md` | when delegated to | Isolated workers |
| `.claude/settings.json` | at startup | Permissions + hook registrations |
| `.claude/release-calendar.json` | read by hooks | **Cycle dates. Update monthly.** |
| `managed-settings.json` | at startup, un-overridable | Org policy |

## 3.3 Docs

memory · settings · permissions · hooks · skills · sub-agents · plugins · plugin-marketplaces ·
mcp · features-overview · costs · large-codebases · admin-setup · analytics · sandboxing ·
best-practices — all at `https://code.claude.com/docs/en/<page>`
