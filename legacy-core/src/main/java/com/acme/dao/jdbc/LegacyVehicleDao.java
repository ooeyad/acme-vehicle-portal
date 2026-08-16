package com.acme.dao.jdbc;

import com.acme.dao.VehicleDao;
import com.acme.domain.Vehicle;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * TRAINING FIXTURE - DO NOT COPY. DO NOT USE IN PRODUCTION CODE.
 *
 * This class deliberately contains the four defects that are most common in the real
 * legacy DAOs. It exists so the team can run the db2-sql-reviewer subagent against a
 * file with known findings and confirm the tooling works before trusting it on real code.
 *
 * Expected findings:
 *   1. SQL built by string concatenation  -> SQL injection.
 *   2. SELECT *                           -> breaks when a column is added or reordered.
 *   3. Unbounded result set               -> whole table can reach the web tier.
 *   4. Connection/Statement/ResultSet never closed -> connection pool exhaustion.
 *
 * Exercise: ask Claude to "review LegacyVehicleDao with the db2-sql-reviewer subagent".
 * It should find all four. Then ask it to rewrite the class following JdbcVehicleDao.
 */
public class LegacyVehicleDao implements VehicleDao {

    private final DataSource dataSource;

    public LegacyVehicleDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Vehicle findByVin(String vin) throws SQLException {
        Connection cn = dataSource.getConnection();
        Statement st = cn.createStatement();
        ResultSet rs = st.executeQuery(
                "SELECT * FROM VEHICLE.VEHICLE WHERE VIN = '" + vin + "'");
        if (rs.next()) {
            return new Vehicle(
                    rs.getString("VIN").trim(),
                    rs.getString("MODEL"),
                    rs.getInt("MODEL_YEAR"),
                    rs.getString("DEALER_CODE"));
        }
        return null;
    }

    @Override
    public List<Vehicle> findByDealerAndYear(String dealerCode, int modelYear, int limit) {
        throw new UnsupportedOperationException(
                "Training fixture. Use JdbcVehicleDao - see this class's header comment.");
    }

    @Override
    public List<Vehicle> findByDealer(String dealerCode, int limit) throws SQLException {
        List<Vehicle> out = new ArrayList<Vehicle>();
        Connection cn = dataSource.getConnection();
        Statement st = cn.createStatement();
        ResultSet rs = st.executeQuery(
                "SELECT * FROM VEHICLE.VEHICLE WHERE DEALER_CODE = '" + dealerCode + "'");
        while (rs.next()) {
            out.add(new Vehicle(
                    rs.getString("VIN").trim(),
                    rs.getString("MODEL"),
                    rs.getInt("MODEL_YEAR"),
                    rs.getString("DEALER_CODE")));
        }
        return out;
    }
}
