package com.randomlychosenbytes.openfoodfactsdumper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Willi
 */
public class Utils {

    public static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+");

    public static float roundToDecimals(float number, int numDecimalPlaces) {
        float factor = (float) Math.pow(10, numDecimalPlaces);
        return Math.round(number * factor) / factor;
    }

    public static String buildUniqueList(String ignore, String filterString, String blacklistArray[], final int maxElementLength, final int maxNumElements) {
        String filterArray[] = filterString.split(",");
        
        // trim
        for (int i = 0; i < filterArray.length; i++) {
            filterArray[i] = filterArray[i].trim();
        }
        
        // unique
        Set<String> set = new HashSet<>();
        set.addAll(Arrays.asList(filterArray));
        
        StringBuilder output = new StringBuilder();

        int i = 0;
        for (String str : set) {

            if (i == maxNumElements) { // three cateogies are enough
                break;
            }

            str = str.trim();
            boolean isNotTheFoodName = !str.equals(ignore);
            boolean isNotOnlyWhiteSpaces = str.replaceAll(" ", "").length() != 0;
            boolean isNotEmpty = !str.isEmpty();
            boolean isLengthOk = str.length() <= maxElementLength;
            boolean isNotOnIgnoreList = !isStringContainedInListElement(str, blacklistArray);

            if (isNotTheFoodName && isNotEmpty
                    && isLengthOk && isNotOnlyWhiteSpaces
                    && isNotOnIgnoreList) {

                if (i != 0) {
                    output.append(", ");
                }

                output.append(str);
                i++;
            }
        }

        return output.toString();
    }

    /**
     * Returns true if the given string is found in the ignore list.
     *
     * @param subject
     * @param list
     * @return
     */
    public static boolean isStringContainedInListElement(String subject, String list[]) {
        for (String ignoreItem : list) {
            if (subject.contains(ignoreItem)) {
                return true;
            }
        }

        return false;
    }
    
    public static long writeToFile(List<Food> foods, String countryName) throws FileNotFoundException, IOException {
        final String FILE_EXPORT_PATH = "new/db_dump_" + countryName.replace(' ', '_').toLowerCase() + ".csv";

        FileOutputStream fOut = new FileOutputStream(FILE_EXPORT_PATH);
        OutputStreamWriter osw = new OutputStreamWriter(fOut, "UTF-8");

        for (Food food : foods) {

            osw.write(food.toCsvLine() + "\n"); // {} is a portion json string
        }

        osw.flush();
        osw.close();

        long fileSizeBytes = new File(FILE_EXPORT_PATH).length();
        return (long) (fileSizeBytes / 1000.0);
    }

    public static Set<Portion> getPortions(String str, String portionTranslation) {
        Set<Portion> portionsSet = new HashSet<>();

        Matcher m = NUMBER_PATTERN.matcher(str);
        while (m.find()) {
            try {
                float portionWeight = new Float(m.group());

                if (portionWeight == 0 || portionWeight == 1 || portionWeight == 100) {
                    break;
                }
                portionsSet.add(new Portion("1 " + portionTranslation, portionWeight));
            } catch (NumberFormatException e) {
                // don't add it
            }

            break;
        }

        return portionsSet;
    }

    /**
     * Searches a string for units used for fluids to determine whether the food
     * is a beverage/fluid. If one is found true is returned, false otherwise.
     *
     * @param quantityStr
     * @return
     */
    public static boolean isBeverage(String quantityStr) {
        quantityStr = quantityStr.toLowerCase();
        return quantityStr.contains("litre") || quantityStr.contains("l") || quantityStr.contains("ml") || quantityStr.contains("cl");
    }

    public static float getWeightInGrams(String quantityStr) throws NumberFormatException {
        quantityStr = quantityStr.toLowerCase();
        float factor = 1;

        if (quantityStr.isEmpty()) {
            return 100f;
        }

        if (quantityStr.contains("kg") || quantityStr.contains("l")) {
            quantityStr = quantityStr.replace("kg", "");
            factor = 1000f;
        }

        if (quantityStr.contains("l")) {
            quantityStr = quantityStr.replace("l", "");
            factor = 1000f;
        }

        if (quantityStr.contains("g")) {
            quantityStr = quantityStr.replace("g", "");
            factor = 1f;
        }

        if (quantityStr.contains("ml")) {
            quantityStr = quantityStr.replace("ml", "");
            factor = 1f;
        }

        if (quantityStr.contains("cl")) {
            quantityStr = quantityStr.replace("g", "");
            factor = 10f;
        }

        return new Float(quantityStr) * factor;
    }
}
