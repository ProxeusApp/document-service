package com.proxeus.document.odt.img;

/**
 * DecimalAndUnit helps us converting the units provided by LibreOffice to pixels.
 * So far only "in" and "pt" were in use and tested.
 */
public class DecimalAndUnit {
    public double number;
    public String unit;

    public String toString() {
        return number + unit;
    }

    public double getWithDPI(double dpi) throws Exception {
        switch (unit) {
            case "cm":
                return number * 2.54 * dpi;
            case "in":
            case "inch":
                return number * dpi;
            case "pt":
                return (number / 75 * (dpi / 96) * 100);
            default:
                throw new Exception("No unit set");
        }
    }
}