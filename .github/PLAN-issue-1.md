# Plan — Issue #1, dealer inventory screen

Produced in plan mode before any file was edited. Reviewed and approved, then implemented.

## Reference patterns being followed

| Layer | Copying |
|---|---|
| DAO | `JdbcVehicleDao.findByDealer` |
| Service | `VehicleService.listForDealer` |
| Form | `VehicleLookupForm` |
| Action | `VehicleLookupAction` |
| JSP | `vehicle/lookup.jsp` + `vehicle/lookupResult.jsp` |
| Service test | `VehicleServiceTest` |
| Action test | `VehicleLookupActionTest` + `MockActionSupport` |

## Files

**Change (3)**

1. `VehicleDao` — **add** `findByDealerAndYear(String, int, int)`.
   Additive: `findByDealer` is left untouched. Widening the existing signature would break every
   caller, and the legacy-safe-change rule is additive-over-invasive even in our own new code.
2. `JdbcVehicleDao` — implement it with a new `FIND_BY_DEALER_AND_YEAR` constant.
   Predicate is `DEALER_CODE = ? AND MODEL_YEAR = ?`, which matches the leading columns of
   `IX_VEHICLE_DEALER (DEALER_CODE, MODEL_YEAR DESC)`, so the index is used.
3. `VehicleService` — **add** `listForDealer(String, Integer, int)` overload; the existing
   2-arg method delegates to it with a null year.

**Create (5)**

4. `DealerInventoryForm` — `dealerCode`, `modelYear` (both String; conversion in the service).
5. `DealerInventoryAction extends BaseAction`.
6. `WEB-INF/jsp/vehicle/dealerInventory.jsp` — search form + results, one screen.
7. `struts-config.xml` — form-bean + action mapping for `/vehicle/inventory`.
8. `MessageResources.properties` — new keys.

**Tests (4)**

9. `VehicleServiceTest` — year filtering, year-bound validation, limit capping with a year.
10. `DealerInventoryFormTest` — structural validation.
11. `DealerInventoryActionTest` — forward chosen + request attributes published.
12. `JdbcVehicleDaoSqlConventionsTest` — **refactor to reflection** (see below).

## Decisions taken in planning

**Model-year bounds are fixed constants, not derived from the clock.**
`MIN_MODEL_YEAR = 1980`, `MAX_MODEL_YEAR = 2100`. Deriving the upper bound from
`Calendar.getInstance()` would make the test suite time-dependent and it would start failing on a
1 January. A fixed upper bound is less precise and far more maintainable.

**Empty result is a success, not a failure.**
A dealer with no vehicles gets the results screen with an empty-state message. Forwarding to
`failure` would render "something went wrong" for a legitimate, common case.

**The conventions test moves to reflection.**
`JdbcVehicleDaoSqlConventionsTest.ALL_QUERIES` is currently a hardcoded array. Adding a query and
forgetting to register it means the query is silently unchecked — precisely the failure a
conventions test exists to prevent. Discovering the gap while adding the first new query is the
argument for fixing it now. Reflection over `static final String` fields, with an assertion that
at least one query was found so the test cannot silently pass against nothing.

## Verification

- `./mvnw -pl legacy-core test` and `./mvnw -pl legacy-web -am test`
- `code-reviewer` subagent over the diff
- `db2-sql-reviewer` subagent over the new SQL
- `security-reviewer` — the screen exposes customer/dealer data, so this is mandatory per CLAUDE.md

## Risk noted before starting

The screen takes a dealer code **as user input**. Unless something ties the requesting user to
their own dealer, any authenticated user can read any dealer's inventory. Flagged here so the
security review has to give a verdict rather than discovering it late.
