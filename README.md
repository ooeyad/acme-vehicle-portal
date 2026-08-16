# ACME Vehicle Portal — Claude Code reference project

A deliberately small Struts 1.3 + DB2 application that exists for one reason: to be the
**reference implementation** every convention in the `acme-standards` plugins points at.

When a skill says "follow the nearest existing pattern", this is where that pattern lives.

## What is here

```
legacy-core/   services, DAOs, domain      <- JdbcVehicleDao is the DAO to copy
legacy-web/    actions, forms, JSPs        <- VehicleLookupAction is the action to copy
db/migration/  DB2 migrations              <- agent edits blocked by a hook
.claude/       CLAUDE.md, rules, settings, release-calendar.json
```

## Build

```bash
./mvnw -pl legacy-core test        # 13 tests
./mvnw -pl legacy-web -am test     # 8 tests
```

Java 8. No database required: the tests use hand-written stubs and a SQL-conventions test that
asserts on the query text, so the whole suite runs offline on a laptop.

> The Java sources and tests in this project were compiled with `--release 8` and all 21 tests
> were executed and passed. They were **not** built through Maven against the real Struts and
> JUnit artifacts, because the machine that generated them had no access to Maven Central. Run
> `./mvnw -pl legacy-core test` once on your network before handing this to the team.

## Four exercises to run on day one

These are how you prove the tooling works before asking anyone to trust it.

**1. The reviewer finds real defects.**
`LegacyVehicleDao` contains four deliberate defects: string-concatenated SQL, `SELECT *`, an
unbounded result set, and leaked JDBC resources.
> "Review LegacyVehicleDao using the db2-sql-reviewer subagent."

It should find all four. Then: *"Rewrite it following JdbcVehicleDao."*

**2. The release cycle is understood.**
```bash
git checkout -b release/2026.08
claude
```
The session should open by telling you it is in stabilization, name the go-live date, and state
that only defect fixes belong here. Then ask it to edit `pom.xml` — a hook blocks it.

**3. The conventions test catches a bad query.**
Ask Claude to add a `findByModelYear` query to `JdbcVehicleDao` without `WITH UR`.
`JdbcVehicleDaoSqlConventionsTest` fails, and Claude fixes it without you saying why.

**4. Characterization tests make legacy code safe.**
> "/acme-struts-db2:characterization-test VehicleService.normaliseVin"

Compare what it writes to the `CHARACTERIZATION:` test already in `VehicleServiceTest`.

## Before you use this for real

Every name here is a placeholder: `acme`, `VP`, `ACMEDEV`, `VEHICLE.VEHICLE`, the Bitbucket and
Jira URLs in `.claude/settings.json` and `.claude/release-calendar.json`. Replace them.
