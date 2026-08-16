# Issue #1 — Dealers cannot see their own inventory

**Type:** Story
**Target iteration:** 2026.09
**Labels:** `enhancement`, `legacy-web`, `iteration:2026.09`

## Problem

A dealer has no way to see which vehicles are assigned to them. They phone head office, someone
runs a query by hand, and reads the list back. This happens several times a week per dealer.

## Proposal

A screen at `/vehicle/inventory` where a dealer enters their dealer code and optionally a model
year, and sees the matching vehicles.

## Acceptance criteria

- [ ] Entering a valid dealer code lists that dealer's vehicles, newest model year first
- [ ] An optional model year narrows the list
- [ ] A dealer with no vehicles sees an empty-state message, not an error
- [ ] An invalid dealer code or model year is rejected with a field-level message
- [ ] The result set is bounded — a dealer with 10,000 vehicles cannot exhaust the web tier
- [ ] Follows the reference patterns: `JdbcVehicleDao`, `VehicleService`, `VehicleLookupAction`

## Out of scope

- Editing vehicles
- CSV export
- Cross-dealer search for head-office staff (separate issue if needed)

## Notes

`VehicleService.listForDealer()` already exists and is tested, but nothing calls it. The DAO can
already filter by dealer; only the model-year filter and the whole web tier are missing.
