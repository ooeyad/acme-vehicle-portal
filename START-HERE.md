# START HERE — one folder, `claude`, go

You take **`acme-vehicle-portal`** and nothing else. Everything is inside it now: the rules, the
hooks, the subagents, the `/` commands. No `--plugin-dir`, no marketplace, no second folder.

---

## Setup (5 minutes, once)

```bash
# 1. Claude Code
curl -fsSL https://claude.ai/install.sh | bash        # macOS / Linux / WSL
# Windows PowerShell:  irm https://claude.ai/install.ps1 | iex

# 2. jq — the hooks parse JSON with it.
#    WITHOUT jq THE HOOKS SILENTLY DO NOTHING. This is the #1 cause of "it's not working".
brew install jq          # macOS
sudo apt install jq      # Debian / Ubuntu / WSL

# 3. Optional: gh (issues + PRs), java + maven (to run the tests)
```

**Native Windows:** also install [Git for Windows](https://git-scm.com/downloads/win) — the hooks
are bash scripts and Claude Code runs them through Git Bash. WSL is the smoother path if you have it.

## Run it

```bash
cd acme-vehicle-portal
git init && git add -A && git commit -m "Initial import"
git checkout -b develop        # the hooks read your branch; develop is where feature work lives

claude
```

That's it. If this appears before you type anything, everything is wired correctly:

```
RELEASE CYCLE: DEVELOPMENT for 2026.09. Branch develop. Code freeze 2026-09-09 (in 24 days).
Feature work belongs here. Fixes for the iteration currently in test go on release/2026.08, not here.
```

Sanity checks, typed in the session: `/context` (CLAUDE.md listed under Memory files) ·
`/hooks` (six hooks) · type `/` (nine commands).

---

## What is actually in the folder

```
acme-vehicle-portal/
├── CLAUDE.md                     loaded every session — the always-on rules
├── legacy-core/  legacy-web/     the application + 42 tests
├── db/migration/                 DB2 migrations (hook-blocked, human-only)
└── .claude/
    ├── settings.json             permissions + hook registrations
    ├── release-calendar.json     iteration dates — the hooks read this
    ├── rules/                    struts.md, db2.md (load only for matching files)
    ├── agents/                   5 subagents
    ├── skills/                   9 slash commands
    ├── hooks/scripts/            6 guardrail scripts
    └── bin/                      acme-backmerge-check
```

### The nine commands

| Command | When |
|---|---|
| `/start-work 12` | Beginning any issue — picks the right base branch for the cycle phase |
| `/spec-first 12` | Bigger feature — interviews you and writes SPEC.md before any code |
| `/pr-ready` | Finished — build, test, review, commit, PR |
| `/fix-defect 12` | Bug during a freeze window — narrow-change discipline |
| `/release-manager freeze\|daily\|golive` | Running a release |
| `/legacy-safe-change` | Reference: how to change old code safely |
| `/characterization-test` | Pin current behaviour before touching untested code |
| `/db2-query-conventions` · `/add-struts-action` | Reference patterns |

### The five subagents

`code-reviewer` · `security-reviewer` · `test-writer` · `struts-explorer` · `db2-sql-reviewer`

Ask for them by name: *"Review this with the db2-sql-reviewer subagent."*

---

## Your first 20 minutes

In order. The first three are read-only — nothing can break.

**1. Ask a question** (2 min)
```
How does a request get from a URL to the database in this application?
```
Checks that CLAUDE.md and the rules are actually grounding it. It should name real classes and the
Action → Service → DAO layering.

**2. Watch a review find real bugs** (5 min)
```
Review LegacyVehicleDao using the db2-sql-reviewer subagent.
```
That class has **four deliberate defects**: concatenated SQL, `SELECT *`, an unbounded result set,
leaked JDBC resources. It should find all four. This is where you decide whether to trust it.

**3. Watch a guardrail block you** (2 min)
```
Add a column to db/migration/V001__create_vehicle.sql
```
Blocked — migrations are DBA-reviewed. The tool call never runs; this isn't Claude choosing to
decline. Try `web.xml` too.

**4. Watch the freeze window change the rules** (3 min)
```bash
git checkout -b release/2026.08
claude
```
Same repo, same day, opposite instructions: defect fixes only, bug ticket required, back-merge
reminder. Ask it to edit `pom.xml` — blocked, because dependency changes aren't defect fixes.
Then `git checkout develop`.

**5. Run the tests** (3 min)
```bash
mvn -pl legacy-core test          # 13
mvn -pl legacy-web -am test       # 8
```
Expect 42 passing. This is the **first real Maven run** — I built this without access to Maven
Central, so the code was compiled and the tests executed against API stubs. The logic is verified,
but if a dependency version needs nudging, that's why. Paste the error at Claude.

---

## Your first real change

```
/start-work 4
```
Then:
> `BaseAction.execute` catches only `RuntimeException`, but its comment says it prevents any
> exception reaching the container. A `SQLException` from any Action escapes today. Fix it, add a
> test that fails without the fix, and check every existing Action for behaviour changes first.

Finish with `/pr-ready`.

Small, genuinely broken, and it touches shared code — so it teaches the blast-radius habit while
the stakes are low.

---

## The daily loop

```
claude              # read the RELEASE CYCLE line
/start-work 12      # right base branch, automatically
                    # Shift+Tab into plan mode for anything touching 2+ files
/pr-ready           # build, test, review, commit, PR
```

`/clear` between unrelated tasks. Every message re-sends the whole conversation, so a session
open since morning costs real money on a one-line question.

---

## Troubleshooting

**No `RELEASE CYCLE:` line**
```bash
jq --version                              # missing? that's it. install jq.
git rev-parse --abbrev-ref HEAD           # in a git repo?
jq . .claude/release-calendar.json        # valid JSON?
```
Then `/hooks` — if SessionStart is empty, `.claude/settings.json` didn't load. Run `/status` to see
which settings sources are active, and make sure you started `claude` from the repo root.

**Hooks don't fire on Windows** — install Git for Windows, or use WSL.

**Commands missing** — type `/` and browse. They're unnamespaced here (`/start-work`, not
`/acme-core:start-work`).

**Maven fails on Java version** — poms target Java 8. Point `JAVA_HOME` at a JDK 8, or bump
`maven.compiler.source`/`target` to `11` in the root `pom.xml`. No Java-8-specific code is used.

**Dates feel wrong** — edit `.claude/release-calendar.json`. It's the one file that drives every
cycle-aware behaviour. Put your real freeze and go-live dates in it.

---

## When the team joins (not yet)

Convert `.claude/` into a plugin, push it as its own repo, and point every repo at it via
`settings.team.json`. Then a teammate clones and gets everything with no setup. That's what
`claude-standards/` in the other zip already is — and the PLAYBOOK is the 8-week plan for that
rollout, plus managed settings, analytics and governance.

**None of that is something you do inside this repo.** Get comfortable here first; you'll want to
have changed half a dozen things in `.claude/` before anyone else depends on them.
