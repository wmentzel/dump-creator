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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenFoodFactsDumpCreator 0.1 Alpha
 * @author wmentzel
 */
public class Dumper {

    private static final String[] ignoreList = new String[]{"//Die Zugehörigkeit zu://, Lidl Stiftung & Co. KG"};
    
    public static String FILE_EXPORT_PATH = "C:\\Users\\Willi\\Downloads\\db_dump_germany.csv";
    public static String FILE_IMPORT_PATH = "C:\\Users\\Willi\\Downloads\\en.openfoodfacts.org.products.csv";

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

                String categoriees = buildUniqueList(foodName, nextLine[FieldNames.categories] + nextLine[FieldNames.generic_name]);
                food.setCategories(categoriees);

                try {
                    food.setCalories(new Integer(nextLine[FieldNames.energy_100g]));
                    food.setWeight(100);
                    food.setProtein(new Float(nextLine[FieldNames.proteins_100g]));
                    food.setCarbohydrate(new Float(nextLine[FieldNames.carbohydrates_100g]));
                    food.setFat(new Float(nextLine[FieldNames.fat_100g]));
                    //food.calculateFor100g();
                } catch (NumberFormatException e) {
                    continue;
                }

                //System.out.println(food.toCsvLine());
                //System.out.println("--------");
                foods.add(food);
                foodCount++;
            }

            System.out.println("-----------------------STATISTICS-----------------------");
            System.out.println("Longest name (" + maxLength + " characters): " + longestFoodName);
            System.out.println("Results: " + foodCount);

            writeToFile(foods);
            long fileSizekB = (int) (new File("C:\\Users\\Willi\\Downloads\\output.csv").length() / 1000.0);

            System.out.println("Dump size: " + fileSizekB + " kB");

        } catch (IOException ex) {
            Logger.getLogger(Dumper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void writeToFile(List<Food> foods) throws FileNotFoundException, IOException {
        FileOutputStream fOut = new FileOutputStream(FILE_EXPORT_PATH);
        OutputStreamWriter osw = new OutputStreamWriter(fOut, "UTF-8");

        final int numFoods = foods.size();

        for (Food food : foods) {

            osw.write(food.toCsvLine() + "|{}"+ "\n");
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
            boolean notOnlyWhiteSpaces = str.replaceAll(" ", "").length() != 0;
            boolean isNotEmpty = !str.isEmpty();
            boolean isLengthOk = str.length() <= 64;
            boolean isNotInIngoreList = !isInIgnoreList(str);

            if (isNotTheFoodName && isNotEmpty
                    && isLengthOk && notOnlyWhiteSpaces
                    && isNotInIngoreList) {

                output.append(str);

                if (i < set.size() - 2) {
                    output.append(", ");
                }
            }

            i++;
        }

        return output.toString();
    }

    public static boolean isInIgnoreList(String subject) {
        for (String str : ignoreList) {
            if (str.equals(ignoreList)) {
                return true;
            }
        }

        return false;
    }
}
