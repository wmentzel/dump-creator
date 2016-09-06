package com.randomlychosenbytes.openfoodfactsdumper;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Food
 */
public class Food {

    static public int FOOD_DOES_NOT_EXIST = -1;

    private String name;
    private String brands;
    private String categories;

    private float weight;
    private int calories;
    private float fat;
    private float carbohydrate;
    private float protein;
    private boolean isBeverage;

    private Set<Portion> portions;

    public Food() {
        name = "";
        brands = "";
        categories = "";
        portions = new HashSet<>();
    }

    public Food(String name, String brand, String categories,
            float weight, int calories, float fat, float carbohydrate, float protein,
            boolean isBeverage) {
        super();
        this.name = name;
        this.brands = brand;
        this.categories = categories;

        this.weight = weight;
        this.calories = calories;
        this.fat = fat;
        this.carbohydrate = carbohydrate;
        this.protein = protein;
        this.isBeverage = isBeverage;
    }

    public float calculateFor100g() {
        float multiplier = 1.0f / (weight / 100.0f);

        calories = Math.round(calories * multiplier);
        fat = Utils.roundToDecimals(fat * multiplier, 1);
        carbohydrate = Utils.roundToDecimals(carbohydrate * multiplier, 1);
        protein = Utils.roundToDecimals(protein * multiplier, 1);

        weight = 100;

        return multiplier;
    }

    /**
     * Returns a string that represents a line in a CSV file.
     *
     * @return CSV line of object
     */
    public String toCsvLine() {
        String fat = String.valueOf(this.fat).replace(",", ".");
        String carbohydrate = String.valueOf(this.carbohydrate).replace(",", ".");
        String protein = String.valueOf(this.protein).replace(",", ".");

        String portionsStr = portions.toString();
        portionsStr = portionsStr.replace("[", "{");
        portionsStr = portionsStr.replace("]", "}");
       
        
        // add portions
        return name + "|" + getBrands() + "|" + getCategories() + "|" + weight + "|" + calories + "|"
                + fat + "|" + carbohydrate + "|" + protein + "|" + isBeverage + "|" + portionsStr;
    }

    public Set<Portion> getPortions() {
        return portions;
    }

    public void setPortions(Set<Portion> portions) {
        this.portions = portions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public float getCarbohydrate() {
        return carbohydrate;
    }

    public void setCarbohydrate(float carbohydrate) {
        this.carbohydrate = carbohydrate;
    }

    public float getProtein() {
        return protein;
    }

    public void setProtein(float protein) {
        this.protein = protein;
    }

    public float getFat() {
        return fat;
    }

    public void setFat(float fat) {
        this.fat = fat;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public boolean isBeverage() {
        return isBeverage;
    }

    public void setBeverage(boolean isBeverage) {
        this.isBeverage = isBeverage;
    }

    public String getBrands() {
        if (this.brands == null) {
            this.brands = "";
        }

        return brands;
    }

    public void setBrands(String brands) {
        this.brands = brands;
    }

    public String getCategories() {
        if (this.categories == null) {
            this.categories = "";
        }

        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public static class FoodComparator implements Comparator<Food> {

        @Override
        public int compare(Food food1, Food food2) {
            return food1.getName().compareTo(food2.getName());
        }
    }
}
