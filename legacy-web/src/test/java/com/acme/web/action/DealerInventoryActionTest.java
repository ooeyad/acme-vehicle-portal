package com.acme.web.action;

import com.acme.dao.VehicleDao;
import com.acme.domain.Vehicle;
import com.acme.service.VehicleService;
import com.acme.web.form.DealerInventoryForm;
import com.acme.web.support.MockActionSupport;

import org.apache.struts.action.ActionForward;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Asserts the Action's whole contract and nothing more: which forward it chose, and what it
 * published to request scope for the JSP. Business rules are tested in VehicleServiceTest.
 */
public class DealerInventoryActionTest {

    private static VehicleService serviceReturning(final List<Vehicle> vehicles) {
        return new VehicleService(new VehicleDao() {
            @Override public Vehicle findByVin(String vin) { return null; }
            @Override public List<Vehicle> findByDealer(String d, int l) { return vehicles; }
            @Override public List<Vehicle> findByDealerAndYear(String d, int y, int l) { return vehicles; }
        });
    }

    private static VehicleService serviceRejectingTheYear() {
        return new VehicleService(new VehicleDao() {
            @Override public Vehicle findByVin(String vin) { return null; }
            @Override public List<Vehicle> findByDealer(String d, int l) { return Collections.emptyList(); }
            @Override public List<Vehicle> findByDealerAndYear(String d, int y, int l) {
                return Collections.emptyList();
            }
        });
    }

    private static DealerInventoryForm formWith(String dealer, String year) {
        DealerInventoryForm f = new DealerInventoryForm();
        f.setDealerCode(dealer);
        f.setModelYear(year);
        return f;
    }

    private static List<Vehicle> vehicles(int count) {
        List<Vehicle> out = new ArrayList<Vehicle>();
        for (int i = 0; i < count; i++) {
            out.add(new Vehicle(String.format("1HGCM82633A%06d", i), "Corolla", 2019, "DLR001"));
        }
        return out;
    }

    @Test
    public void publishesTheVehiclesAndForwardsToSuccess() throws Exception {
        DealerInventoryAction action = new DealerInventoryAction(serviceReturning(vehicles(3)));
        MockActionSupport.RequestState state = MockActionSupport.newRequestState();

        ActionForward forward = action.execute(MockActionSupport.mapping(),
                formWith("DLR001", null),
                MockActionSupport.request(state),
                MockActionSupport.response());

        assertEquals("success", forward.getName());
        assertNotNull(state.attributes.get("vehicles"));
        assertEquals(Integer.valueOf(3), state.attributes.get("resultCount"));
        assertEquals("DLR001", state.attributes.get("dealerCode"));
    }

    /** An empty result is a legitimate answer, not an error - the JSP renders an empty state. */
    @Test
    public void anEmptyResultStillForwardsToSuccess() throws Exception {
        DealerInventoryAction action =
                new DealerInventoryAction(serviceReturning(new ArrayList<Vehicle>()));
        MockActionSupport.RequestState state = MockActionSupport.newRequestState();

        ActionForward forward = action.execute(MockActionSupport.mapping(),
                formWith("DLR999", null),
                MockActionSupport.request(state),
                MockActionSupport.response());

        assertEquals("success", forward.getName());
        assertEquals(Integer.valueOf(0), state.attributes.get("resultCount"));
        assertEquals(Boolean.FALSE, state.attributes.get("truncated"));
    }

    @Test
    public void flagsTheResultAsTruncatedWhenThePageIsFull() throws Exception {
        DealerInventoryAction action =
                new DealerInventoryAction(serviceReturning(vehicles(DealerInventoryAction.PAGE_SIZE)));
        MockActionSupport.RequestState state = MockActionSupport.newRequestState();

        action.execute(MockActionSupport.mapping(), formWith("DLR001", null),
                MockActionSupport.request(state), MockActionSupport.response());

        assertEquals("the JSP tells the user to narrow the search",
                Boolean.TRUE, state.attributes.get("truncated"));
    }

    @Test
    public void aModelYearOutsideTheSupportedRangeForwardsToFailure() throws Exception {
        DealerInventoryAction action = new DealerInventoryAction(serviceRejectingTheYear());
        MockActionSupport.RequestState state = MockActionSupport.newRequestState();

        ActionForward forward = action.execute(MockActionSupport.mapping(),
                formWith("DLR001", "1800"),
                MockActionSupport.request(state),
                MockActionSupport.response());

        assertEquals("failure", forward.getName());
        assertTrue("nothing is published when the request was rejected",
                state.attributes.get("vehicles") == null);
    }
}
