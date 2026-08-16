---
paths:
  - "**/*Dao.java"
  - "**/*DAO.java"
  - "**/*.sql"
---

# DB2 conventions

Schemas: `VEHICLE`, `DEALER`, `ORDERS`, `REF`.

1. Parameter markers (`?`) for every value. Never concatenate.
2. Name the columns. Never `SELECT *`.
3. `FETCH FIRST n ROWS ONLY` on anything reaching the web tier.
4. `WITH UR` on read-only queries, to avoid lock escalation on the large `ORDERS` tables.
5. try-with-resources for Connection, PreparedStatement, ResultSet. Leaked connections are the
   most common real defect in this codebase.
6. Explicit transaction boundaries in the service. Never rely on autocommit across statements.
7. No DDL in application code — migrations only.

Type traps: `VIN` is `CHAR(17)` and blank-padded, so pad on write and trim on read (see
`JdbcVehicleDao`). `DECIMAL` vs `INTEGER` comparisons suppress index use. Timestamps are UTC.

Every DAO gets a conventions test — copy `JdbcVehicleDaoSqlConventionsTest`.
