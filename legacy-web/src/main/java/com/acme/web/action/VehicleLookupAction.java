package com.acme.web.action;

import com.acme.domain.Vehicle;
import com.acme.domain.VehicleNotFoundException;
import com.acme.service.VehicleService;
import com.acme.web.form.VehicleLookupForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * REFERENCE IMPLEMENTATION. Model new Actions on this one.
 *
 * Note what it does NOT do: no SQL, no DAO construction, no business rules, no
 * connection handling, no logging of the VIN. Four steps and a forward.
 */
public class VehicleLookupAction extends BaseAction {

    private final VehicleService vehicleService;

    /** Production constructor: Struts instantiates Actions with a no-arg constructor. */
    public VehicleLookupAction() {
        this(ServiceLocator.vehicleService());
    }

    /** Test constructor: lets a test inject a service without a container. */
    VehicleLookupAction(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Override
    protected ActionForward doExecute(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        VehicleLookupForm lookupForm = (VehicleLookupForm) form;

        try {
            Vehicle vehicle = vehicleService.lookup(lookupForm.getVin());
            request.setAttribute("vehicle", vehicle);
            return mapping.findForward(SUCCESS);

        } catch (VehicleNotFoundException e) {
            addError(request, "error.vin.notfound", e.getVin());
            return mapping.findForward(FAILURE);

        } catch (IllegalArgumentException e) {
            addError(request, "error.vin.length");
            return mapping.findForward(FAILURE);
        }
    }
}
