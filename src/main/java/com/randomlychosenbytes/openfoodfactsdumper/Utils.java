package com.randomlychosenbytes.openfoodfactsdumper;

import static com.randomlychosenbytes.openfoodfactsdumper.Dumper.FILE_EXPORT_PATH;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Willi
 */
public class Utils {

    public static float roundToDecimals(float number, int numDecimalPlaces) {
        float factor = (float) Math.pow(10, numDecimalPlaces);
        return Math.round(number * factor) / factor;
    }

    public static String buildUniqueList(String ignore, String filterString, String blacklistArray[], final int maxElementLength, final int maxNumElements) {
        String filterArray[] = filterString.split(",");

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
            }

            i++;
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
    
    public static long writeToFile(List<Food> foods) throws FileNotFoundException, IOException {
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
}
