# OpenFoodFactsDumpCreator

The next release of Calories Count, an Android app I have written to track your daily calories, 
will include subsets of the data provided by openfoodfacts.org

The database is, with only ~224 MB (all countries, all columns), so small right now, that it makes sense to download all foods for one country, which enables
the app to function completely offline once the download is done.

A subset will only contain the foods available in the chosen country and will only have the columns that CC utilizes which makes it really small (kB).

**Column names:**
foodName|brands|categories|calories_100g|fat_100g|carbs_100g|protein_100g|beverage|portions**

**Example:**<br/>
Coca Cola|Coca Cola||18|85.2|4.0|0.4|true|{"1 Bottle":500}**

This little Java programm will create this subset by parsing the complete CSV file provided by openfoodfacts.org, 
which contains nutritional information from all over the world. The filtered result will be saved as CSV file as well which can be imported in CC.