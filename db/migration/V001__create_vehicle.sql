-- DB2 migration. HUMAN-AUTHORED AND DBA-REVIEWED.
-- Agent sessions are blocked from editing this directory by a PreToolUse hook.
--
-- Naming: V<nnn>__<snake_case_description>.sql, applied in order, never edited once merged.
-- A correction is a new migration, not a change to an existing one.

CREATE TABLE VEHICLE.VEHICLE (
    VIN          CHAR(17)     NOT NULL,
    MODEL        VARCHAR(60)  NOT NULL,
    MODEL_YEAR   SMALLINT     NOT NULL,
    DEALER_CODE  CHAR(8),
    CREATED_TS   TIMESTAMP    NOT NULL DEFAULT CURRENT TIMESTAMP,
    CONSTRAINT PK_VEHICLE PRIMARY KEY (VIN)
);

-- Supports findByDealer: leading column must match the predicate or the index is not used.
CREATE INDEX VEHICLE.IX_VEHICLE_DEALER
    ON VEHICLE.VEHICLE (DEALER_CODE, MODEL_YEAR DESC);

COMMENT ON TABLE VEHICLE.VEHICLE IS 'Vehicles visible to the dealer portal';
