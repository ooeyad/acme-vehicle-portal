package com.acme.dao.jdbc;

import com.acme.dao.VehicleDao;
import com.acme.domain.Vehicle;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * REFERENCE IMPLEMENTATION. This is the DAO every new DAO should be modelled on.
 *
 * It demonstrates the five conventions Claude and reviewers check for:
 *   1. Parameter markers (?) for every value. Never string concatenation.
 *   2. Named columns. Never SELECT *.
 *   3. A bound result set. FETCH FIRST n ROWS ONLY on anything reaching the web tier.
 *   4. WITH UR on read-only queries, to avoid lock escalation on the large tables.
 *   5. try-with-resources so Connection, PreparedStatement and ResultSet always close.
 */
public class JdbcVehicleDao implements VehicleDao {

    static final String FIND_BY_VIN =
            "SELECT VIN, MODEL, MODEL_YEAR, DEALER_CODE "
          + "FROM VEHICLE.VEHICLE "
          + "WHERE VIN = ? "
          + "FETCH FIRST 1 ROWS ONLY "
          + "WITH UR";

    static final String FIND_BY_DEALER =
            "SELECT VIN, MODEL, MODEL_YEAR, DEALER_CODE "
          + "FROM VEHICLE.VEHICLE "
          + "WHERE DEALER_CODE = ? "
          + "ORDER BY MODEL_YEAR DESC, VIN "
          + "FETCH FIRST ? ROWS ONLY "
          + "WITH UR";

    /**
     * Predicate order matches the leading columns of IX_VEHICLE_DEALER
     * (DEALER_CODE, MODEL_YEAR DESC), so DB2 uses the index rather than scanning.
     */
    static final String FIND_BY_DEALER_AND_YEAR =
            "SELECT VIN, MODEL, MODEL_YEAR, DEALER_CODE "
          + "FROM VEHICLE.VEHICLE "
          + "WHERE DEALER_CODE = ? AND MODEL_YEAR = ? "
          + "ORDER BY VIN "
          + "FETCH FIRST ? ROWS ONLY "
          + "WITH UR";

    private final DataSource dataSource;

    public JdbcVehicleDao(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource is required");
        }
        this.dataSource = dataSource;
    }

    @Override
    public Vehicle findByVin(String vin) throws SQLException {
        if (vin == null) {
            return null;
        }
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_VIN)) {

            // VIN is CHAR(17): pad so the comparison matches DB2's blank-padded storage.
            ps.setString(1, pad(vin.trim(), 17));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<Vehicle> findByDealer(String dealerCode, int limit) throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        List<Vehicle> out = new ArrayList<Vehicle>();
        if (dealerCode == null) {
            return out;
        }
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_DEALER)) {

            ps.setString(1, dealerCode.trim());
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    @Override
    public List<Vehicle> findByDealerAndYear(String dealerCode, int modelYear, int limit)
            throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        List<Vehicle> out = new ArrayList<Vehicle>();
        if (dealerCode == null) {
            return out;
        }
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_DEALER_AND_YEAR)) {

            ps.setString(1, dealerCode.trim());
            ps.setInt(2, modelYear);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    private static Vehicle map(ResultSet rs) throws SQLException {
        return new Vehicle(
                trim(rs.getString("VIN")),
                trim(rs.getString("MODEL")),
                rs.getInt("MODEL_YEAR"),
                trim(rs.getString("DEALER_CODE")));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
