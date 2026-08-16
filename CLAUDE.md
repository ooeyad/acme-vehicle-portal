# ACME Vehicle Portal (legacy)

Struts 1.3 + Java 8 + DB2 on WebSphere. In production since 2009. Thinly tested, widely depended on.
Source and issues on GitHub. Monthly iterations with a code freeze; see
`.claude/release-calendar.json` for the current dates.

## Build and test

```bash
./mvnw -pl legacy-core test                          # core tests, fast
./mvnw -pl legacy-web -am test                       # web tests + dependencies
./mvnw -q -pl legacy-core test -Dtest=VehicleServiceTest   # single test - prefer this
./mvnw -pl legacy-web -am clean package              # build the WAR
```
Never run `mvn clean install` at the root. Requires Java 8 (`JAVA_HOME` on the 1.8 JDK) and the
DB2 client on `PATH`. DEV database alias is `ACMEDEV`; there is no local database.

## Layout

- `legacy-core/` — services, DAOs, domain objects
- `legacy-web/` — Struts actions, form beans, JSPs, `struts-config.xml`
- `db/migration/` — DB2 migrations (**human-authored, DBA-reviewed; agent edits are blocked**)

## Reference implementations — copy these, do not invent a new pattern

| For | Copy |
|---|---|
| A DAO | `JdbcVehicleDao` |
| A service | `VehicleService` |
| An action | `VehicleLookupAction` |
| A form bean | `VehicleLookupForm` |
| A JSP | `WEB-INF/jsp/vehicle/lookup.jsp` |
| A service test | `VehicleServiceTest` |
| An action test | `VehicleLookupActionTest` + `MockActionSupport` |
| SQL convention test | `JdbcVehicleDaoSqlConventionsTest` |

`LegacyVehicleDao` is a **training fixture containing deliberate defects**. Never copy it and
never use it in production code.

## Conventions

- Actions parse the request, call one `*Service`, set request attributes, return a forward.
  No business logic, no SQL, no DAO construction in an Action.
- Services own rules and transactions. DAOs own SQL. Nothing skips a layer.
- Parameter markers (`?`) in every query. Never build SQL by concatenation.
- Named columns, `FETCH FIRST n ROWS ONLY`, `WITH UR` on read-only queries.
- try-with-resources for every Connection, PreparedStatement and ResultSet.
- User-visible text goes through `MessageResources.properties`, never inline in a JSP.
- Every new DAO gets a SQL conventions test like `JdbcVehicleDaoSqlConventionsTest`.

## Branching and tickets

Every change traces to a GitHub issue. Branch names carry the issue number and drive our tooling:
`feature/12-slug`, `bugfix/12-slug`, `hotfix/12-slug`. The base branch depends on the issue type
and where we are in the release cycle — use `/acme-core:start-work 12` rather than deciding by hand.

Commit messages reference the issue so GitHub links them: `#12 <imperative summary>`. Do **not**
write `fixes #12` or `closes #12` in a commit — that auto-closes the issue on merge, before anyone
has tested it. Humans close issues.

## Never

- Never edit `db/migration/**`, `web.xml`, `target/**`, or vendored code.
- Never connect to a production DB2 alias, and never run DDL from code or the shell.
- Never push directly to `main`, `develop`, or a `release/*` branch.
- Never log a VIN or dealer credential.

## Compaction

When compacting, preserve the list of modified files, the test commands run, the ticket key, and
any characterization findings about surprising current behaviour.
