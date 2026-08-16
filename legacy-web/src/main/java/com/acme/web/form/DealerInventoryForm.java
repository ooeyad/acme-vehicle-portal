package com.acme.web.form;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import javax.servlet.http.HttpServletRequest;

/**
 * Form bean for the dealer inventory screen. Modelled on VehicleLookupForm.
 *
 * Both fields are String because that is what arrives on a request. Model year is parsed in
 * getModelYearOrNull() rather than by Struts, so a non-numeric value produces a field message
 * instead of a container-level conversion failure.
 */
public class DealerInventoryForm extends ActionForm {

    private static final long serialVersionUID = 1L;

    private String dealerCode;
    private String modelYear;

    public String getDealerCode() { return dealerCode; }
    public void setDealerCode(String dealerCode) { this.dealerCode = dealerCode; }

    public String getModelYear() { return modelYear; }
    public void setModelYear(String modelYear) { this.modelYear = modelYear; }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.dealerCode = null;
        this.modelYear = null;
    }

    /** @return the parsed model year, or null when the field was left blank. */
    public Integer getModelYearOrNull() {
        if (modelYear == null || modelYear.trim().isEmpty()) {
            return null;
        }
        return Integer.valueOf(modelYear.trim());
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();

        String dealer = dealerCode == null ? "" : dealerCode.trim();
        if (dealer.isEmpty()) {
            errors.add("dealerCode", new ActionMessage("error.dealer.required"));
        } else if (dealer.length() > 8 || !dealer.toUpperCase().matches("[A-Z0-9]+")) {
            errors.add("dealerCode", new ActionMessage("error.dealer.format"));
        }

        // Model year is optional, but if supplied it must be four digits.
        String year = modelYear == null ? "" : modelYear.trim();
        if (!year.isEmpty() && !year.matches("[0-9]{4}")) {
            errors.add("modelYear", new ActionMessage("error.modelYear.format"));
        }

        return errors;
    }
}
