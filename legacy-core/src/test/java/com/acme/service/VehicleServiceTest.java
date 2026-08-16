package com.acme.service;

import com.acme.dao.VehicleDao;
import com.acme.domain.Vehicle;
import com.acme.domain.VehicleNotFoundException;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * REFERENCE TEST. Model new service tests on this one.
 *
 * Note there is no mocking framework. The DAO is a hand-written stub, because a stub you
 * can read in ten seconds beats a mock you have to decode. "Do not mock what you can construct."
 */
public class VehicleServiceTest {

    private static final String VALID_VIN = "1HGCM82633A004352";

    /** Hand-written stub. Records what it was asked for so tests can assert on it. */
    private static final class StubVehicleDao implements VehicleDao {
        private final Vehicle toReturn;
        private final List<Vehicle> listToReturn;
        String lastVinRequested;
        String lastDealerRequested;
        int lastLimitRequested;
        Integer lastYearRequested;
        boolean yearMethodCalled;

        StubVehicleDao(Vehicle toReturn, List<Vehicle> listToReturn) {
            this.toReturn = toReturn;
            this.listToReturn = listToReturn;
        }

        @Override
        public Vehicle findByVin(String vin) {
            this.lastVinRequested = vin;
            return toReturn;
        }

        @Override
        public List<Vehicle> findByDealer(String dealerCode, int limit) {
            this.lastDealerRequested = dealerCode;
            this.lastLimitRequested = limit;
            return listToReturn;
        }

        @Override
        public List<Vehicle> findByDealerAndYear(String dealerCode, int modelYear, int limit) {
            this.lastDealerRequested = dealerCode;
            this.lastLimitRequested = limit;
            this.lastYearRequested = Integer.valueOf(modelYear);
            this.yearMethodCalled = true;
            return listToReturn;
        }
    }

    private static Vehicle sampleVehicle() {
        return new Vehicle(VALID_VIN, "Corolla", 2019, "DLR001");
    }

    @Test
    public void lookupReturnsTheVehicleForAKnownVin() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(sampleVehicle(), null);
        VehicleService service = new VehicleService(dao);

        Vehicle found = service.lookup(VALID_VIN);

        assertEquals(VALID_VIN, found.getVin());
        assertEquals("Corolla", found.getModel());
    }

    @Test
    public void lookupUppercasesAndTrimsBeforeHittingTheDatabase() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(sampleVehicle(), null);
        VehicleService service = new VehicleService(dao);

        service.lookup("  1hgcm82633a004352  ");

        assertEquals("The DAO must receive a normalised VIN", VALID_VIN, dao.lastVinRequested);
    }

    @Test
    public void lookupThrowsWhenTheVinIsUnknown() throws SQLException {
        VehicleService service = new VehicleService(new StubVehicleDao(null, null));
        try {
            service.lookup(VALID_VIN);
            fail("expected VehicleNotFoundException");
        } catch (VehicleNotFoundException expected) {
            assertEquals(VALID_VIN, expected.getVin());
        }
    }

    /**
     * CHARACTERIZATION: a malformed VIN throws IllegalArgumentException rather than
     * returning null or throwing VehicleNotFoundException. Callers - including
     * VehicleLookupAction - depend on telling these two cases apart so the user sees
     * "check the VIN" rather than "not found". Preserved deliberately.
     */
    @Test
    public void lookupRejectsAVinThatIsNotSeventeenCharacters() throws Exception {
        VehicleService service = new VehicleService(new StubVehicleDao(sampleVehicle(), null));
        try {
            service.lookup("TOOSHORT");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("17"));
        }
    }

    @Test
    public void listForDealerCapsTheLimitSoTheWebTierCannotAskForEverything() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, new ArrayList<Vehicle>());
        VehicleService service = new VehicleService(dao);

        service.listForDealer("dlr001", 100000);

        assertEquals(VehicleService.MAX_DEALER_RESULTS, dao.lastLimitRequested);
        assertEquals("Dealer code must be normalised", "DLR001", dao.lastDealerRequested);
    }

    @Test
    public void listForDealerAppliesADefaultLimitWhenNoneIsRequested() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, new ArrayList<Vehicle>());
        new VehicleService(dao).listForDealer("DLR001", 0);
        assertEquals(50, dao.lastLimitRequested);
    }

    @Test
    public void listForDealerReturnsEmptyRatherThanNullForABlankDealer() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, Arrays.asList(sampleVehicle()));
        assertTrue(new VehicleService(dao).listForDealer("   ", 10).isEmpty());
    }

    // --- issue #1: optional model-year filter -------------------------------------------------

    @Test
    public void aNullModelYearUsesTheUnfilteredQuery() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, new ArrayList<Vehicle>());
        new VehicleService(dao).listForDealer("DLR001", null, 10);
        assertFalse("a null year must not reach the year-filtered query", dao.yearMethodCalled);
    }

    @Test
    public void aModelYearSwitchesToTheFilteredQueryAndIsPassedThrough() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, new ArrayList<Vehicle>());
        new VehicleService(dao).listForDealer("dlr001", Integer.valueOf(2019), 10);

        assertTrue(dao.yearMethodCalled);
        assertEquals(Integer.valueOf(2019), dao.lastYearRequested);
        assertEquals("dealer code is normalised on both paths", "DLR001", dao.lastDealerRequested);
    }

    @Test
    public void theLimitIsCappedOnTheYearFilteredPathToo() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, new ArrayList<Vehicle>());
        new VehicleService(dao).listForDealer("DLR001", Integer.valueOf(2019), 100000);
        assertEquals(VehicleService.MAX_DEALER_RESULTS, dao.lastLimitRequested);
    }

    @Test
    public void aModelYearBeforeTheSupportedRangeIsRejected() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, new ArrayList<Vehicle>());
        try {
            new VehicleService(dao).listForDealer("DLR001", Integer.valueOf(1979), 10);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("1980"));
        }
    }

    @Test
    public void aModelYearAfterTheSupportedRangeIsRejected() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, new ArrayList<Vehicle>());
        try {
            new VehicleService(dao).listForDealer("DLR001", Integer.valueOf(2101), 10);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("2100"));
        }
    }

    @Test
    public void theRangeBoundariesThemselvesAreAccepted() throws Exception {
        StubVehicleDao dao = new StubVehicleDao(null, new ArrayList<Vehicle>());
        VehicleService service = new VehicleService(dao);
        service.listForDealer("DLR001", Integer.valueOf(VehicleService.MIN_MODEL_YEAR), 10);
        service.listForDealer("DLR001", Integer.valueOf(VehicleService.MAX_MODEL_YEAR), 10);
        assertEquals(Integer.valueOf(VehicleService.MAX_MODEL_YEAR), dao.lastYearRequested);
    }
}
