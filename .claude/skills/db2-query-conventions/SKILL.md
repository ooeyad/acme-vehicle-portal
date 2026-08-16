---
name: db2-query-conventions
description: ACME DB2 SQL and JDBC conventions - parameter markers, isolation levels, paging, resource handling, and the schema layout. Use when writing or modifying any SQL, stored-procedure call, or JDBC code against DB2.
paths:
  - "**/*.sql"
  - "**/*Dao.java"
  - "**/*DAO.java"
  - "**/*Repository.java"
---

# DB2 conventions at ACME

<!-- Replace the schema/table names below with your real ones before rollout. -->

## Schema layout
| Schema | Contents | Notes |
|---|---|---|
| `VEHICLE` | VIN, model, build, options | `VEHICLE.VIN` is CHAR(17) — mind CHAR padding in comparisons |
| `DEALER` | Dealers, territories, contacts | Row-level access enforced by view, not by the app |
| `ORDERS` | Orders, allocations, status history | Partitioned by order date |
| `REF` | Reference and lookup tables | Cacheable; changes rarely |

## Non-negotiable
1. **Parameter markers only.** `?` for every value, including values that "come from our own code".
   Never build SQL by concatenation.
2. **No `SELECT *`.** Name the columns.
3. **Bound every read.** `FETCH FIRST n ROWS ONLY` on anything reaching a web tier.
4. **Read-only reports use `WITH UR`.** Uncommitted read avoids lock escalation on the large
   `ORDERS` tables. Do not use it where the caller needs committed data.
5. **Close everything.** try-with-resources, or a `finally` block that closes `ResultSet`,
   `PreparedStatement`, and `Connection` in that order. This is the most common real defect in the
   legacy DAOs — check it every time.
6. **Explicit transaction boundaries.** Never rely on autocommit across multiple statements.
7. **No DDL from application code.** Schema changes are migration files, reviewed by a DBA.

## Type traps
- `CHAR` is blank-padded. `WHERE VIN = ?` with a trimmed value silently matches nothing. Trim on
  read, pad or use `RTRIM()` consistently on write — and follow whatever the neighbouring DAO does.
- `DECIMAL` vs `INTEGER` comparisons cause an implicit cast that suppresses index use.
- Timestamps are stored in UTC. Convert at the presentation layer, never in SQL.

## Stored procedures
Existing procedures live in `VEHICLE` and `ORDERS`. Call them with `CallableStatement` and register
out-parameters explicitly. Do not add a new stored procedure without DBA sign-off — propose the
change, do not write it.

## Before finishing
Run the `db2-sql-reviewer` subagent on any new or modified SQL.
