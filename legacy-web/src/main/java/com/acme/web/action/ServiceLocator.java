package com.acme.web.action;

import com.acme.dao.jdbc.JdbcVehicleDao;
import com.acme.service.VehicleService;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Minimal service lookup for the legacy web tier.
 *
 * The application predates dependency injection. Rather than each Action building its
 * own DAO (which is how connection leaks spread), everything goes through here.
 * Do not add business logic to this class, and do not cache a Connection in it.
 */
final class ServiceLocator {

    private static final String DS_JNDI = "java:comp/env/jdbc/AcmeVehicleDS";

    private static volatile VehicleService vehicleService;

    private ServiceLocator() { }

    static VehicleService vehicleService() {
        VehicleService local = vehicleService;
        if (local == null) {
            synchronized (ServiceLocator.class) {
                local = vehicleService;
                if (local == null) {
                    local = new VehicleService(new JdbcVehicleDao(dataSource()));
                    vehicleService = local;
                }
            }
        }
        return local;
    }

    private static DataSource dataSource() {
        try {
            return (DataSource) new InitialContext().lookup(DS_JNDI);
        } catch (NamingException e) {
            throw new IllegalStateException("DataSource " + DS_JNDI + " is not bound", e);
        }
    }
}
