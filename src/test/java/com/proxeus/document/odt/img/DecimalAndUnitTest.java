package com.proxeus.document.odt.img;

import org.junit.Assert;
import org.junit.Test;

/**
 * Project: document-service
 * User: awerffeli
 * Date: 06.03.20
 * Time: 13:40
 **/
public class DecimalAndUnitTest {


    @Test
    public void testToString() {
        DecimalAndUnit decimalAndUnit = new DecimalAndUnit();
        decimalAndUnit.number = 1;
        decimalAndUnit.unit = "cm";


        Assert.assertEquals("1.0cm", decimalAndUnit.toString());
    }

    @Test
    public void testGetWithDPI() {
        DecimalAndUnit decimalAndUnit = new DecimalAndUnit();
        decimalAndUnit.number = 1;
        decimalAndUnit.unit = "cm";


        double withDPI = 0;
        try {
            withDPI  = decimalAndUnit.getWithDPI(2.0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertEquals(5.08, withDPI, 0);


        decimalAndUnit.unit = "in";
        try {
            withDPI  = decimalAndUnit.getWithDPI(2.0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertEquals(2.0, withDPI, 0);


        decimalAndUnit.unit = "pt";
        try {
            withDPI  = decimalAndUnit.getWithDPI(2.0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertEquals(0.027777777777777776, withDPI, 0);


        decimalAndUnit.unit = "";
        try {
            decimalAndUnit.getWithDPI(2.0);
        } catch (Exception e) {
            //success
            return;
        }
        Assert.fail();
    }
}
