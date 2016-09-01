package com.randomlychosenbytes.openfoodfactsdumper;

/**
 *
 * @author Willi
 */
public class Utils {

    public static float roundToDecimals(float number, int numDecimalPlaces) {
        float factor = (float) Math.pow(10, numDecimalPlaces);
        return Math.round(number * factor) / factor;
    }
}
