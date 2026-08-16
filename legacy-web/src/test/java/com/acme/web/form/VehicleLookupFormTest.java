package com.acme.web.form;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Form validation is structural only: required, length, character set.
 * Business validation lives in VehicleService and is tested there.
 */
public class VehicleLookupFormTest {

    private static VehicleLookupForm formWith(String vin) {
        VehicleLookupForm f = new VehicleLookupForm();
        f.setVin(vin);
        return f;
    }

    @Test
    public void acceptsAWellFormedVin() {
        assertTrue(formWith("1HGCM82633A004352").validate(null, null).isEmpty());
    }

    @Test
    public void rejectsAnEmptyVinWithASingleError() {
        assertEquals(1, formWith("   ").validate(null, null).size());
    }

    @Test
    public void rejectsAVinOfTheWrongLength() {
        assertTrue(formWith("SHORT").validate(null, null).size() > 0);
    }

    /** VINs exclude I, O and Q so they cannot be confused with 1 and 0. */
    @Test
    public void rejectsTheLettersExcludedFromTheVinAlphabet() {
        assertTrue(formWith("1HGCM82633A0043IO").validate(null, null).size() > 0);
    }

    @Test
    public void resetClearsTheVinSoAStaleValueCannotLeakIntoTheNextRequest() {
        VehicleLookupForm f = formWith("1HGCM82633A004352");
        f.reset(null, null);
        assertEquals(null, f.getVin());
    }
}
