package com.acme.web.form;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Structural validation only: required, length, character set, numeric format.
 * Range checking for the model year lives in VehicleService and is tested there.
 */
public class DealerInventoryFormTest {

    private static DealerInventoryForm formWith(String dealer, String year) {
        DealerInventoryForm f = new DealerInventoryForm();
        f.setDealerCode(dealer);
        f.setModelYear(year);
        return f;
    }

    @Test
    public void acceptsADealerCodeWithNoYear() {
        assertTrue(formWith("DLR001", null).validate(null, null).isEmpty());
    }

    @Test
    public void acceptsADealerCodeWithAFourDigitYear() {
        assertTrue(formWith("DLR001", "2019").validate(null, null).isEmpty());
    }

    @Test
    public void requiresADealerCode() {
        assertEquals(1, formWith("   ", null).validate(null, null).size());
    }

    @Test
    public void rejectsADealerCodeThatIsTooLong() {
        assertTrue(formWith("DLR0012345", null).validate(null, null).size() > 0);
    }

    @Test
    public void rejectsADealerCodeWithPunctuation() {
        assertTrue(formWith("DLR-001", null).validate(null, null).size() > 0);
    }

    @Test
    public void rejectsANonNumericModelYear() {
        assertTrue(formWith("DLR001", "twenty").validate(null, null).size() > 0);
    }

    @Test
    public void rejectsAModelYearThatIsNotFourDigits() {
        assertTrue(formWith("DLR001", "19").validate(null, null).size() > 0);
    }

    @Test
    public void treatsABlankModelYearAsAbsentRatherThanInvalid() {
        assertTrue(formWith("DLR001", "   ").validate(null, null).isEmpty());
        assertNull(formWith("DLR001", "   ").getModelYearOrNull());
    }

    @Test
    public void parsesTheModelYearWhenPresent() {
        assertEquals(Integer.valueOf(2019), formWith("DLR001", " 2019 ").getModelYearOrNull());
    }

    @Test
    public void resetClearsBothFieldsSoStaleValuesCannotLeakIntoTheNextRequest() {
        DealerInventoryForm f = formWith("DLR001", "2019");
        f.reset(null, null);
        assertNull(f.getDealerCode());
        assertNull(f.getModelYear());
    }
}
