# #1 Add dealer inventory screen

> **STATUS: DRAFT — BLOCKED.** Do not merge. Security review raised a Critical
> authorization gap that cannot be fixed inside this diff. See "Blocker" below.
> Opened as a draft so the implementation can be reviewed while #2 is scoped.

## What changed

A screen at `/vehicle/inventory` where a dealer enters their dealer code and optionally a model
year, and sees the matching vehicles. Replaces phoning head office to have the query run by hand.

| Layer | Change |
|---|---|
| DAO | `findByDealerAndYear` **added** (not folded into `findByDealer` — widening an existing signature breaks every caller) |
| SQL | `FIND_BY_DEALER_AND_YEAR`; predicate order matches `IX_VEHICLE_DEALER (DEALER_CODE, MODEL_YEAR DESC)` so the index is used |
| Service | `listForDealer(String, Integer, int)` overload; the 2-arg method delegates with a null year |
| Web | `DealerInventoryForm`, `DealerInventoryAction`, `dealerInventory.jsp`, `struts-config` mapping, message keys |
| Tests | 21 new tests (42 total, all passing) |

## Why

Issue #1. `VehicleService.listForDealer()` already existed and was tested but nothing called it —
only the model-year filter and the web tier were missing.

## How it was verified

```
./mvnw -pl legacy-core test        13 tests
./mvnw -pl legacy-web -am test      8 tests
                                   --------
                                   42 tests, 42 passed
```

The SQL conventions test was deliberately proven to fail: writing the query the naive way
(`SELECT *`, no `FETCH FIRST`, no `WITH UR`) fails three assertions with the reason in the message.

## Blast radius

- `VehicleDao` gained a method, so **every implementer must change**. `JdbcVehicleDao` implements
  it; `LegacyVehicleDao` (training fixture) throws `UnsupportedOperationException`; the anonymous
  stub in `VehicleLookupActionTest` gained the method. The compiler found all three — this is the
  argument for the interface rather than an abstract class.
- `BaseAction` gained an **additive** `addFieldError` overload. No existing behaviour changed.
- No schema change. No dependency change. No pipeline change.
- `/vehicle/inventory.do` is matched by the existing `/vehicle/*` security constraint, so the URL
  is not anonymously reachable. That is **authentication only** — see the blocker.

## Blocker — Critical, from security review

**Any authenticated dealer can read any other dealer's inventory.** The dealer code is taken
verbatim from the request and nothing ties the caller to a dealer — the application never reads
`getUserPrincipal()` anywhere. A dealer can enumerate `[A-Z0-9]{1,8}` codes and pull up to 50 VINs
per hit. Container security cannot mitigate this: `web.xml` grants `dealer` and `staff` identical
access and has no concept of *which* dealer.

The fix needs a principal → dealer-code mapping that **does not exist in this codebase**. That is
issue #2 and it is the real size of this feature.

Note: the correct `web.xml` change is also blocked from the agent by a `PreToolUse` hook — that
file requires a human with security review, by design.

## Review findings addressed in this diff

| # | Severity | Finding | Resolution |
|---|---|---|---|
| 1 | Critical | No dealer authorization (IDOR) | **Not fixable here** → issue #2, blocks merge |
| 2 | Warning | `web.xml` lacks `user-data-constraint` / `login-config` | → issue #3, security team, human-only file |
| 3 | Warning | VIN rendered as a link → VIN in URL, access logs, `Referer` | **Fixed** — VIN is plain text |
| 4 | Warning | `SQLException` escapes to container, can leak SQLCODE/schema | **Fixed** in this action → issue #4 for `BaseAction` |
| 5 | Note | No `Cache-Control: no-store` on a VIN list | → issue #5, belongs in a servlet filter |
| 6 | Note | Out-of-range year gave a global, not field-level, message | **Fixed** — `addFieldError` |

## Follow-up issues to raise

- **#2 — Map authenticated users to dealer codes** *(blocks #1)*
- **#3 — Add `user-data-constraint` and `login-config` to web.xml** *(security team)*
- **#4 — `BaseAction` catches only `RuntimeException` despite its comment** *(affects every action)*
- **#5 — `no-store` cache filter for `/vehicle/*`**

## Release cycle

Targets **2026.09**. Branched from `develop`, not `release/2026.08` — 2026.08 is in stabilization
until 19 Aug and accepts defect fixes only. No back-merge needed: nothing landed on a release branch.
