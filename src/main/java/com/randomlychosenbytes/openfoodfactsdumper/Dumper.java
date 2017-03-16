package com.randomlychosenbytes.openfoodfactsdumper;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * OpenFoodFactsDumpCreator 0.1 Alpha
 * Query open food facts database for coutnries and the number of food available for them
 * SELECT countries_en, COUNT(*) as count from foods GROUP BY countries_en ORDER BY count DESC
 *
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

        List<CSVRecord> csvRecords = new LinkedList<>();
        int numErrors = 0;

        try {
            FileReader fileReader = new FileReader(FILE_IMPORT_PATH);
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withSkipHeaderRecord().withDelimiter('\t');
            CSVParser csvFileParser = new CSVParser(fileReader, csvFileFormat);

            Iterator<CSVRecord> it = csvFileParser.iterator();

            boolean hasNext = true;

            while(hasNext){
                try {
                    hasNext = it.hasNext();
                    csvRecords.add(it.next());
                } catch (Exception ex) {
                    numErrors++;
                    ex.printStackTrace();
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        int index = 0;

        for (String countryName : COUNTRIES) {
            int foodCountAfterFiltering = 0;
            int foodCountForCountry = 0;

            int maxLength = 0;
            String longestFoodName = "";

            Map<String, Food> foodNameBrandMap = new HashMap<>();

            for (CSVRecord record : csvRecords) {

                if (record.size() < 159) { // is the line invalid?
                    continue;
                }

                if (!record.get(FieldNames.countries_en).contains(countryName)) {
                    continue;
                }

                foodCountForCountry++;

                //
                // Create the food object
                //
                Food food = new Food();

                String foodName = record.get(FieldNames.product_name).trim().replace("&quot;", "");

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
                food.setBrands(Utils.buildUniqueList(foodName, record.get(FieldNames.brands),
                        CATEGORIES_BLACKLIST, MAX_LENGTH_BRAND_NAME, MAX_NUM_BRANDS));

                String categoriees = Utils.buildUniqueList(foodName, record.get(FieldNames.categories_en) + "," + record.get(FieldNames.generic_name),
                        CATEGORIES_BLACKLIST, MAX_LENGTH_CATEGORY_NAME, MAX_NUM_CATEGORIES);
                food.setCategories(categoriees);
                food.setBeverage(Utils.isBeverage(record.get(FieldNames.quantity)));

                try {
                    int kiloJoule = new Integer(record.get(FieldNames.energy_100g));
                    food.setCalories(Math.round(kiloJoule * KILOJOULE_TO_KCAL_FACTOR));
                    food.setWeight(100.0f);
                    food.setProtein(new Float(record.get(FieldNames.proteins_100g)));
                    food.setCarbohydrate(new Float(record.get(FieldNames.carbohydrates_100g)));
                    food.setFat(new Float(record.get(FieldNames.fat_100g)));
                } catch (NumberFormatException e) {
                    continue;
                }

                Set<Portion> portionSet = Utils.getPortions(record.get(FieldNames.serving_size), PORTION_TRANSLATION[index]);
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

            try {
                System.out.println("Dump size: " + Utils.writeToFile(uniqueFilteredFoods, countryName) + " kB");
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("____________________________________________________________");

            index++;
        }

        System.out.println(numErrors + " lines were not formatted properly!");
    }
}
