package com.acme.dao;

import com.acme.domain.Vehicle;

import java.sql.SQLException;
import java.util.List;

/**
 * Data access for the VEHICLE schema.
 *
 * DAOs own SQL and nothing else. No business rules, no transaction management,
 * no logging of VIN values.
 */
public interface VehicleDao {

    /** @return the vehicle, or null when the VIN is unknown. */
    Vehicle findByVin(String vin) throws SQLException;

    /** @return at most {@code limit} vehicles for the dealer, newest model year first. */
    List<Vehicle> findByDealer(String dealerCode, int limit) throws SQLException;

    /**
     * @return at most {@code limit} vehicles for the dealer in the given model year, VIN order.
     *
     * Added rather than folded into {@link #findByDealer} on purpose: widening an existing
     * signature breaks every caller, and additive-over-invasive applies to our own code too.
     */
    List<Vehicle> findByDealerAndYear(String dealerCode, int modelYear, int limit) throws SQLException;
}
