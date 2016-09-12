package com.randomlychosenbytes.openfoodfactsdumper;

import com.opencsv.CSVReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    public static final float KILOJOULE_TO_KCAL_FACTOR = 0.239006f;

    public static final String COUNTRY = "usa";
    public static final String FILE_EXPORT_PATH = "db_dump_" + COUNTRY + ".csv";
    public static final String FILE_IMPORT_PATH = "en.openfoodfacts.org.products.csv";

    //
    // Constraints
    //
    public static final int MAX_NUM_CATEGORIES = 3;
    public static final int MAX_LENGTH_CATEGORY_NAME = 64;

    public static final int MAX_LENGTH_FOOD_NAME = 64;

    public static final int MAX_LENGTH_BRAND_NAME = 64;
    public static final int MAX_NUM_BRANDS = 3;

    private static final String[] CATEGORIES_BLACKLIST = new String[]{"zu://", "//Property://", "fr:", "en:"};

    private static Map<String, String[]> COUNTRY_SYNONYMS_MAP;

    static {
        COUNTRY_SYNONYMS_MAP = new HashMap<>();
        COUNTRY_SYNONYMS_MAP.put("germany", new String[]{"Germany", "Deutschland", "en:DE"});
        COUNTRY_SYNONYMS_MAP.put("usa", new String[]{"United States", "U.S.A", "usa", "us", "en:US"});
        COUNTRY_SYNONYMS_MAP.put("france", new String[]{"France", "fr:FR"});
        COUNTRY_SYNONYMS_MAP.put("spain", new String[]{"Spain", "es:ES", "Espanol"});
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+");

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

        Map<String, Food> foodNameBrandMap = new HashMap<>();

        try {
            while ((nextLine = reader.readNext()) != null) {

                if (nextLine.length < 159) { // is the line invalid?
                    continue;
                }

                if (!countryMatches(COUNTRY, nextLine[FieldNames.countries])) {
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

                if (foodName.length() > MAX_LENGTH_FOOD_NAME) {
                    continue;
                }

                food.setName(foodName);
                food.setBrands(Utils.buildUniqueList("", nextLine[FieldNames.brands], CATEGORIES_BLACKLIST, MAX_LENGTH_BRAND_NAME, MAX_NUM_BRANDS));

                String categoriees = Utils.buildUniqueList(foodName, nextLine[FieldNames.categories] + "," + nextLine[FieldNames.generic_name], CATEGORIES_BLACKLIST, MAX_LENGTH_CATEGORY_NAME, MAX_NUM_CATEGORIES);
                food.setCategories(categoriees);
                food.setBeverage(isBeverage(nextLine[FieldNames.quantity]));

                try {
                    int kiloJoule = new Integer(nextLine[FieldNames.energy_100g]);
                    food.setCalories(Math.round(kiloJoule * KILOJOULE_TO_KCAL_FACTOR));
                    food.setWeight(100.0f);
                    food.setProtein(new Float(nextLine[FieldNames.proteins_100g]));
                    food.setCarbohydrate(new Float(nextLine[FieldNames.carbohydrates_100g]));
                    food.setFat(new Float(nextLine[FieldNames.fat_100g]));
                } catch (NumberFormatException e) {
                    continue;
                }

                //System.out.println(food.toCsvLine());
                Set<Portion> portionSet = getPortions(nextLine[FieldNames.serving_size]);
                food.setPortions(portionSet);

                foodNameBrandMap.put(food.getName() + food.getBrands(), food);
                foodCount++;
            }

            System.out.println("-------------------------------STATISTICS-------------------------------");
            System.out.println("Longest name: " + longestFoodName + " (" + maxLength + " characters)");
            
            List<Food> foods = new LinkedList<>(foodNameBrandMap.values());
            System.out.println("Number of results: " + foodCount);
            System.out.println("Number of unique results: " + foods.size());

            Collections.sort(foods, new Food.FoodComparator());
            System.out.println("Dump size: " + Utils.writeToFile(foods) + " kB");

        } catch (IOException ex) {
            Logger.getLogger(Dumper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static boolean countryMatches(String countryCode, String countryColumn) {
        String countrySynonyms[] = COUNTRY_SYNONYMS_MAP.get(countryCode);

        for (String synonym : countrySynonyms) {
            if (countryColumn.contains(synonym)) {
                return true;
            }
        }

        return false;
    }

    private static Set<Portion> getPortions(String str) {
        Set<Portion> portionsSet = new HashSet<>();

        Matcher m = NUMBER_PATTERN.matcher(str);
        while (m.find()) {
            try {
                float portionWeight = new Float(m.group());

                if (portionWeight == 0 || portionWeight == 1 || portionWeight == 100) {
                    break;
                }
                portionsSet.add(new Portion("1 Portion", portionWeight));
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

}
