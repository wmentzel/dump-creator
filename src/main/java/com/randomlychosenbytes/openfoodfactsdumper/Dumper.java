package com.randomlychosenbytes.openfoodfactsdumper;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenFoodFactsDumpCreator 0.1 Alpha
 *
 * @author wmentzel
 */
public class Dumper {

    private static final String[] blackList = new String[]{"zu://", "//Property://", "fr:", "en:"};

    public static String FILE_EXPORT_PATH = "db_dump_germany.csv";
    public static String FILE_IMPORT_PATH = "en.openfoodfacts.org.products.csv";

    public static final int MAX_NUM_CATEGORIES = 3;

    private static final Pattern p = Pattern.compile("-?\\d+");

    public static void main(String args[]) {
        CSVReader reader;

        try {
            FileInputStream fis = new FileInputStream(FILE_IMPORT_PATH);
            reader = new CSVReader(new InputStreamReader(fis, "UTF-8"), '\t');
        } catch (FileNotFoundException ex) {
            System.out.println("The " + FILE_IMPORT_PATH + " could not be found!");
            return;
        } catch (UnsupportedEncodingException ex) {
            System.out.println("The chosen file is not encoded in UTF8");
            return;
        }

        String[] nextLine;
        int foodCount = 0;

        int maxLength = 0;
        String longestFoodName = "";

        List<Food> foods = new LinkedList<>();

        try {
            while ((nextLine = reader.readNext()) != null) {

                if (nextLine.length < 159) { // is the line invalid?
                    continue;
                }

                String countries = nextLine[FieldNames.countries];

                boolean countryMatches = countries.contains("Germany") || countries.contains("Deutschland") || countries.contains("en:DE");

                if (!countryMatches) {
                    continue;
                }

                //
                // Create the food object
                //
                Food food = new Food();

                String foodName = nextLine[FieldNames.product_name];

                foodName = foodName.trim();

                // remove unwanted stuff
                foodName = foodName.replace("&quot;", "");

                if (foodName.isEmpty()) {
                    continue;
                }

                if (foodName.length() > maxLength) {
                    maxLength = foodName.length();
                    longestFoodName = foodName;
                }

                if (foodName.length() > 55) {
                    continue;
                }

                food.setName(foodName);
                food.setBrands(buildUniqueList("", nextLine[FieldNames.brands]));

                String categoriees = buildUniqueList(foodName, nextLine[FieldNames.categories] + "," + nextLine[FieldNames.generic_name]);
                food.setCategories(categoriees);
                food.setBeverage(isBeverage(nextLine[FieldNames.quantity]));

                try {
                    food.setCalories(new Integer(nextLine[FieldNames.energy_100g]));
                    food.setWeight(getWeightInGrams(nextLine[FieldNames.quantity]));
                    food.setProtein(new Float(nextLine[FieldNames.proteins_100g]));
                    food.setCarbohydrate(new Float(nextLine[FieldNames.carbohydrates_100g]));
                    food.setFat(new Float(nextLine[FieldNames.fat_100g]));
                    food.calculateFor100g();
                } catch (NumberFormatException e) {
                    continue;
                }

                //System.out.println(food.toCsvLine());
                Set<Portion> portionSet = getPortions(nextLine[FieldNames.serving_size]);
                food.setPortions(portionSet);

                foods.add(food);
                foodCount++;
            }

            System.out.println("-------------------------------STATISTICS-------------------------------");
            System.out.println("Longest name: " + longestFoodName + " (" + maxLength + " characters)");
            System.out.println("Number of results: " + foodCount);

            Collections.sort(foods, new Food.FoodComparator());
            writeToFile(foods);
            long fileSizekB = (long) (new File("C:\\Users\\Willi\\Downloads\\output.csv").length() / 1000.0);

            System.out.println("Dump size: " + fileSizekB + " kB");

        } catch (IOException ex) {
            Logger.getLogger(Dumper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Set<Portion> getPortions(String str) {
        Set<Portion> portionsSet = new HashSet<>();

        Matcher m = p.matcher(str);
        while (m.find()) {
            try {
                float portionWeight = new Float(m.group());
                if (portionWeight == 1 || portionWeight == 100 || portionWeight == 0) {
                    break;
                }
                portionsSet.add(new Portion("1 Portion", portionWeight));
            } catch (NumberFormatException e) {
            }

            break;
        }

        return portionsSet;
    }

    private static boolean isBeverage(String quantityStr) {
        return quantityStr.contains("l") || quantityStr.contains("ml") || quantityStr.contains("cl");
    }

    private static float getWeightInGrams(String quantityStr) throws NumberFormatException {
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

    private static void writeToFile(List<Food> foods) throws FileNotFoundException, IOException {
        FileOutputStream fOut = new FileOutputStream(FILE_EXPORT_PATH);
        OutputStreamWriter osw = new OutputStreamWriter(fOut, "UTF-8");

        for (Food food : foods) {

            osw.write(food.toCsvLine() + "\n"); // {} is a portion json string
        }

        osw.flush();
        osw.close();
    }

    private static String buildUniqueList(String ignore, String filterString) {
        String filterArray[] = filterString.split(",");

        Set<String> set = new HashSet<>();
        set.addAll(Arrays.asList(filterArray));

        StringBuilder output = new StringBuilder();

        int i = 0;
        for (String str : set) {

            if (i == 3) { // three cateogies are enough
                break;
            }

            str = str.trim();
            boolean isNotTheFoodName = !str.equals(ignore);
            boolean isNotOnlyWhiteSpaces = str.replaceAll(" ", "").length() != 0;
            boolean isNotEmpty = !str.isEmpty();
            boolean isLengthOk = str.length() <= 64;
            boolean isNotOnIgnoreList = !isStringContainedInListElement(str, blackList);

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
}
