package com.acme.web.action;

import com.acme.domain.Vehicle;
import com.acme.service.VehicleService;
import com.acme.web.form.DealerInventoryForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;

/**
 * Lists the vehicles assigned to a dealer, optionally narrowed to one model year.
 *
 * Modelled on VehicleLookupAction: read the form, call one service, set request attributes,
 * return a forward. No business logic, no SQL, no DAO construction.
 */
public class DealerInventoryAction extends BaseAction {

    /** Page size for the screen. The service caps this again; both limits are deliberate. */
    static final int PAGE_SIZE = 50;

    private final VehicleService vehicleService;

    public DealerInventoryAction() {
        this(ServiceLocator.vehicleService());
    }

    DealerInventoryAction(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Override
    protected ActionForward doExecute(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        DealerInventoryForm inventoryForm = (DealerInventoryForm) form;

        try {
            List<Vehicle> vehicles = vehicleService.listForDealer(
                    inventoryForm.getDealerCode(),
                    inventoryForm.getModelYearOrNull(),
                    PAGE_SIZE);

            // An empty result is a legitimate answer, not an error: the JSP shows an empty state.
            request.setAttribute("vehicles", vehicles);
            request.setAttribute("dealerCode", inventoryForm.getDealerCode());
            request.setAttribute("modelYear", inventoryForm.getModelYear());
            request.setAttribute("resultCount", Integer.valueOf(vehicles.size()));
            request.setAttribute("truncated", Boolean.valueOf(vehicles.size() >= PAGE_SIZE));

            return mapping.findForward(SUCCESS);

        } catch (IllegalArgumentException e) {
            // Thrown by VehicleService for a model year outside the supported range.
            // Field-level so it renders next to the input, per the issue's acceptance criteria.
            addFieldError(request, "modelYear", "error.modelYear.range");
            return mapping.findForward(FAILURE);

        } catch (SQLException e) {
            // BaseAction only catches RuntimeException, so without this a DB2 failure reaches the
            // container and the error page can leak SQLCODE and the VEHICLE schema name.
            // BaseAction has the same gap for every action - tracked as issue #4.
            addError(request, "error.unexpected");
            return mapping.findForward(FAILURE);
        }
    }
}
