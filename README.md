# OpenFoodFactsDumpCreator

[Calories Count](https://play.google.com/store/apps/details?id=com.randomlychosenbytes.caloriescount), an Android app to
track your daily calories, will include subsets of the data provided
by [Open Food Facts](https://world.openfoodfacts.org/)

This program creates country-specific CSV files which only contain the columns needed for Calories Count. Those files
can then by downloaded by Calories Count, which enables the app to function completely offline once the download is
done.

**Columns of CSV file:**

    foodName|brands|categories|calories_100g|fat_100g|carbs_100g|protein_100g|beverage|portions**

**Example:**

    Coca Cola|Coca Cola||18|85.2|4.0|0.4|true|{"1 Bottle":500}**