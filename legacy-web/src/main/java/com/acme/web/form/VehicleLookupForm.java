package com.acme.web.form;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import javax.servlet.http.HttpServletRequest;

/**
 * Form bean for the VIN lookup screen.
 *
 * Fields are String because that is what arrives on a request. Conversion and
 * business validation happen in the service. validate() does structural checks only:
 * required, length, character set.
 */
public class VehicleLookupForm extends ActionForm {

    private static final long serialVersionUID = 1L;

    private String vin;

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.vin = null;
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        String v = vin == null ? "" : vin.trim();

        if (v.isEmpty()) {
            errors.add("vin", new ActionMessage("error.vin.required"));
            return errors;
        }
        if (v.length() != 17) {
            errors.add("vin", new ActionMessage("error.vin.length"));
        }
        // VIN excludes I, O and Q to avoid confusion with 1 and 0.
        if (!v.toUpperCase().matches("[A-HJ-NPR-Z0-9]+")) {
            errors.add("vin", new ActionMessage("error.vin.characters"));
        }
        return errors;
    }
}
