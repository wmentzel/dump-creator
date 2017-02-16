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
 * Query open food facts database for coutnries and the number of food available for them
 * SELECT countries_en, COUNT(*) as count from foods GROUP BY countries_en ORDER BY count DESC
 * @author wmentzel
 */
public class Dumper {

    public static final float KILOJOULE_TO_KCAL_FACTOR = 0.239006f;

    // countries for which OFF has at least 1000 foods
    public static final String COUNTRIES[] = {
            "United States",
            "Germany",
            "France",
            "Spain",
            "United Kingdom",
            "Belgium",
            "Switzerland",
            "Australia",
            "Italy"
    };

    public static final String PORTION_TRANSLATION[] = {
            "portion",
            "Portion",
            "portion",
            "parte",
            "portion",
            "deel",
            "Portion",
            "portion",
            "porzione"
    };

    public static final String FILE_IMPORT_PATH = "en.openfoodfacts.org.products.csv";

    //
    // Constraints
    //
    public static final int MAX_NUM_CATEGORIES = 3;
    public static final int MAX_LENGTH_CATEGORY_NAME = 64;
    public static final int MAX_LENGTH_FOOD_NAME = 64;
    public static final int MAX_LENGTH_BRAND_NAME = 64;
    public static final int MAX_NUM_BRANDS = 3;

    private static final String[] CATEGORIES_BLACKLIST = new String[]{"fr:", "en:", "de:", "//"};

    public static void main(String args[]) {
        CSVReader reader;

        int index = 0;
        for (String countryName : COUNTRIES) {

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
            int foodCountAfterFiltering = 0;
            int foodCountForCountry = 0;

            int maxLength = 0;
            String longestFoodName = "";

            Map<String, Food> foodNameBrandMap = new HashMap<>();

            try {
                while ((nextLine = reader.readNext()) != null) {

                    if (nextLine.length < 159) { // is the line invalid?
                        continue;
                    }

                    if (!nextLine[FieldNames.countries_en].contains(countryName)){
                        continue;
                    }

                    foodCountForCountry++;

                    //
                    // Create the food object
                    //
                    Food food = new Food();

                    String foodName = nextLine[FieldNames.product_name].trim().replace("&quot;", "");

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
                    food.setBrands(Utils.buildUniqueList(foodName, nextLine[FieldNames.brands],
                            CATEGORIES_BLACKLIST, MAX_LENGTH_BRAND_NAME, MAX_NUM_BRANDS));

                    String categoriees = Utils.buildUniqueList(foodName, nextLine[FieldNames.categories_en] + "," + nextLine[FieldNames.generic_name],
                            CATEGORIES_BLACKLIST, MAX_LENGTH_CATEGORY_NAME, MAX_NUM_CATEGORIES);
                    food.setCategories(categoriees);
                    food.setBeverage(Utils.isBeverage(nextLine[FieldNames.quantity]));

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

                    Set<Portion> portionSet = Utils.getPortions(nextLine[FieldNames.serving_size], PORTION_TRANSLATION[index]);
                    food.setPortions(portionSet);

                    foodNameBrandMap.put(food.getName() + food.getBrands(), food);
                    foodCountAfterFiltering++;
                }

                System.out.println(countryName.toUpperCase());
                System.out.println("Longest food name: " + longestFoodName + " (" + maxLength + " characters)");

                List<Food> uniqueFilteredFoods = new LinkedList<>(foodNameBrandMap.values());
                System.out.println("Number of foods overall = " + foodCountForCountry);
                System.out.println(String.format("Number of foods after filtering = %d (%d are unique)", foodCountAfterFiltering, uniqueFilteredFoods.size()));

                Collections.sort(uniqueFilteredFoods, new Food.FoodComparator());
                System.out.println("Dump size: " + Utils.writeToFile(uniqueFilteredFoods, countryName) + " kB");
                System.out.println("____________________________________________________________");
            } catch (IOException ex) {
                Logger.getLogger(Dumper.class.getName()).log(Level.SEVERE, null, ex);
            }

            index++;
        }
    }
}
