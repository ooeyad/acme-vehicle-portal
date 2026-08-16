package com.acme.domain;

/**
 * Thrown when a VIN does not resolve to a vehicle.
 *
 * Checked on purpose: the caller must decide what the user sees. Actions catch this
 * and forward to "failure"; they never let it escape to the container.
 */
public class VehicleNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String vin;

    public VehicleNotFoundException(String vin) {
        super("No vehicle for VIN " + vin);
        this.vin = vin;
    }

    public String getVin() { return vin; }
}
