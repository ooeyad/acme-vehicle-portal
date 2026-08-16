package com.acme.service;

import com.acme.dao.VehicleDao;
import com.acme.domain.Vehicle;
import com.acme.domain.VehicleNotFoundException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Business rules for vehicle lookup.
 *
 * Services own the rules and the transaction boundary. Actions call services;
 * services call DAOs. Nothing skips a layer.
 */
public class VehicleService {

    /** Anything above this is a caller mistake, not a user request. */
    static final int MAX_DEALER_RESULTS = 200;

    /**
     * Model-year bounds are fixed constants rather than derived from the current date on purpose:
     * deriving the upper bound from the clock makes the test suite time-dependent and it starts
     * failing on a 1 January. Less precise, far more maintainable.
     */
    static final int MIN_MODEL_YEAR = 1980;
    static final int MAX_MODEL_YEAR = 2100;

    private final VehicleDao vehicleDao;

    public VehicleService(VehicleDao vehicleDao) {
        if (vehicleDao == null) {
            throw new IllegalArgumentException("vehicleDao is required");
        }
        this.vehicleDao = vehicleDao;
    }

    /**
     * @throws VehicleNotFoundException when the VIN is unknown - the caller decides
     *         what the user sees.
     */
    public Vehicle lookup(String vin) throws VehicleNotFoundException, SQLException {
        String normalised = normaliseVin(vin);
        Vehicle vehicle = vehicleDao.findByVin(normalised);
        if (vehicle == null) {
            throw new VehicleNotFoundException(normalised);
        }
        return vehicle;
    }

    public List<Vehicle> listForDealer(String dealerCode, int requestedLimit) throws SQLException {
        return listForDealer(dealerCode, null, requestedLimit);
    }

    /**
     * @param modelYear optional; null lists every year for the dealer.
     * @throws IllegalArgumentException when the model year is outside the supported range - the
     *         caller decides what the user sees, consistently with {@link #lookup(String)}.
     */
    public List<Vehicle> listForDealer(String dealerCode, Integer modelYear, int requestedLimit)
            throws SQLException {
        if (dealerCode == null || dealerCode.trim().isEmpty()) {
            return Collections.emptyList();
        }
        int limit = requestedLimit <= 0 ? 50 : Math.min(requestedLimit, MAX_DEALER_RESULTS);
        String normalisedDealer = dealerCode.trim().toUpperCase();

        if (modelYear == null) {
            return vehicleDao.findByDealer(normalisedDealer, limit);
        }
        return vehicleDao.findByDealerAndYear(normalisedDealer, validateModelYear(modelYear), limit);
    }

    static int validateModelYear(int modelYear) {
        if (modelYear < MIN_MODEL_YEAR || modelYear > MAX_MODEL_YEAR) {
            throw new IllegalArgumentException(
                    "model year must be between " + MIN_MODEL_YEAR + " and " + MAX_MODEL_YEAR
                  + ", got " + modelYear);
        }
        return modelYear;
    }

    /**
     * VINs are stored uppercase and unpadded. Reject anything that is not 17 characters
     * so a malformed VIN never reaches the database.
     */
    static String normaliseVin(String vin) {
        if (vin == null) {
            throw new IllegalArgumentException("vin is required");
        }
        String v = vin.trim().toUpperCase();
        if (v.length() != 17) {
            throw new IllegalArgumentException("vin must be 17 characters, got " + v.length());
        }
        return v;
    }
}
// urgent fix
