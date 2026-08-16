package com.acme.web.action;

import com.acme.dao.VehicleDao;
import com.acme.domain.Vehicle;
import com.acme.service.VehicleService;
import com.acme.web.form.VehicleLookupForm;
import com.acme.web.support.MockActionSupport;

import org.apache.struts.action.ActionForward;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * REFERENCE ACTION TEST. Model new action tests on this one.
 *
 * Asserts two things and nothing else: which forward the Action chose, and what it put
 * in request scope. Those are the Action's entire contract - everything else belongs
 * to the service, and is tested there.
 */
public class VehicleLookupActionTest {

    private static final String VALID_VIN = "1HGCM82633A004352";

    private static VehicleService serviceReturning(final Vehicle vehicle) {
        return new VehicleService(new VehicleDao() {
            @Override public Vehicle findByVin(String vin) { return vehicle; }
            @Override public List<Vehicle> findByDealer(String d, int l) { return Collections.emptyList(); }
            @Override public List<Vehicle> findByDealerAndYear(String d, int y, int l) { return Collections.emptyList(); }
        });
    }

    private static VehicleLookupForm formWith(String vin) {
        VehicleLookupForm form = new VehicleLookupForm();
        form.setVin(vin);
        return form;
    }

    @Test
    public void forwardsToSuccessAndPublishesTheVehicleWhenTheVinIsKnown() throws Exception {
        Vehicle vehicle = new Vehicle(VALID_VIN, "Corolla", 2019, "DLR001");
        VehicleLookupAction action = new VehicleLookupAction(serviceReturning(vehicle));
        MockActionSupport.RequestState state = MockActionSupport.newRequestState();

        ActionForward forward = action.execute(
                MockActionSupport.mapping(),
                formWith(VALID_VIN),
                MockActionSupport.request(state),
                MockActionSupport.response());

        assertEquals("success", forward.getName());
        assertNotNull("the JSP reads request-scoped 'vehicle'", state.attributes.get("vehicle"));
        assertEquals(VALID_VIN, ((Vehicle) state.attributes.get("vehicle")).getVin());
    }

    @Test
    public void forwardsToFailureAndPublishesNothingWhenTheVinIsUnknown() throws Exception {
        VehicleLookupAction action = new VehicleLookupAction(serviceReturning(null));
        MockActionSupport.RequestState state = MockActionSupport.newRequestState();

        ActionForward forward = action.execute(
                MockActionSupport.mapping(),
                formWith(VALID_VIN),
                MockActionSupport.request(state),
                MockActionSupport.response());

        assertEquals("failure", forward.getName());
        assertNull(state.attributes.get("vehicle"));
    }

    /**
     * A malformed VIN and an unknown VIN both forward to "failure" but must produce
     * different messages. This test pins that they take the two distinct catch branches.
     */
    @Test
    public void forwardsToFailureWhenTheVinIsMalformed() throws Exception {
        VehicleLookupAction action = new VehicleLookupAction(serviceReturning(null));
        MockActionSupport.RequestState state = MockActionSupport.newRequestState();

        ActionForward forward = action.execute(
                MockActionSupport.mapping(),
                formWith("TOOSHORT"),
                MockActionSupport.request(state),
                MockActionSupport.response());

        assertEquals("failure", forward.getName());
        assertNull(state.attributes.get("vehicle"));
    }
}
