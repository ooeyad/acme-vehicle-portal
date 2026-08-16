package com.acme.domain;

/**
 * A vehicle as the portal understands it.
 *
 * VIN is CHAR(17) in DB2 and therefore blank-padded on read. Always construct
 * with a trimmed VIN; {@link com.acme.dao.jdbc.JdbcVehicleDao} trims on the way out.
 */
public final class Vehicle {

    private final String vin;
    private final String model;
    private final int modelYear;
    private final String dealerCode;

    public Vehicle(String vin, String model, int modelYear, String dealerCode) {
        if (vin == null || vin.trim().isEmpty()) {
            throw new IllegalArgumentException("vin is required");
        }
        this.vin = vin.trim();
        this.model = model;
        this.modelYear = modelYear;
        this.dealerCode = dealerCode == null ? null : dealerCode.trim();
    }

    public String getVin() { return vin; }
    public String getModel() { return model; }
    public int getModelYear() { return modelYear; }
    public String getDealerCode() { return dealerCode; }

    @Override
    public String toString() {
        return "Vehicle[" + vin + " " + modelYear + " " + model + "]";
    }
}
