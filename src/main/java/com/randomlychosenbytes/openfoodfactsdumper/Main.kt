package com.randomlychosenbytes.openfoodfactsdumper

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.FileReader
import java.io.IOException
import java.util.*

/**
 * OpenFoodFactsDumpCreator 0.1 Alpha
 * Query open food facts database for coutnries and the number of food available for them
 * SELECT countries_en, COUNT(*) as count from foods GROUP BY countries_en ORDER BY count DESC

 * @author wmentzel
 */

val KILOJOULE_TO_KCAL_FACTOR = 0.239006f
val COUNTRIES = arrayOf("United States", "Germany", "France", "Spain", "United Kingdom", "Belgium", "Switzerland", "Australia", "Italy")
val PORTION_TRANSLATION = arrayOf("portion", "Portion", "portion", "parte", "portion", "deel", "Portion", "portion", "porzione")
val FILE_IMPORT_PATH = "en.openfoodfacts.org.products.csv"

//
// Constraints
//
val MAX_NUM_CATEGORIES = 3
val MAX_LENGTH_CATEGORY_NAME = 64
val MAX_LENGTH_FOOD_NAME = 64
val MAX_LENGTH_BRAND_NAME = 64
val MAX_NUM_BRANDS = 3
val CATEGORIES_BLACKLIST = arrayOf("fr:", "en:", "de:", "//")

fun main(args: Array<String>) {
    val csvRecords = mutableListOf<CSVRecord>()
    var numErrors = 0

    try {
        val fileReader = FileReader(FILE_IMPORT_PATH)
        val csvFileFormat = CSVFormat.DEFAULT.withSkipHeaderRecord().withDelimiter('\t')
        val csvFileParser = CSVParser(fileReader, csvFileFormat)

        val it = csvFileParser.iterator()

        var hasNext = true

        while (hasNext) {
            try {
                hasNext = it.hasNext()
                csvRecords.add(it.next())
            } catch (ex: Exception) {
                numErrors++
                ex.printStackTrace()
            }

        }

    } catch (ex: IOException) {
        ex.printStackTrace()
        return
    }

    var index = 0

    for (countryName in COUNTRIES) {
        var foodCountAfterFiltering = 0
        var foodCountForCountry = 0

        var maxLength = 0
        var longestFoodName = ""

        val foodNameBrandMap = HashMap<String, Food>()

        for (record in csvRecords) {

            if (record.size() < 159) { // is the line invalid?
                continue
            }

            if (!record.get(FieldNames.countries_en).contains(countryName)) {
                continue
            }

            foodCountForCountry++

            //
            // Create the food object
            //
            val food = Food()

            val foodName = record.get(FieldNames.product_name).trim().replace("&quot;", "")

            if (foodName.isEmpty()) {
                continue
            }

            if (foodName.length > maxLength) {
                maxLength = foodName.length
                longestFoodName = foodName
            }

            if (foodName.length > MAX_LENGTH_FOOD_NAME) {
                continue
            }

            food.name = foodName
            food.brands = Utils.buildUniqueList(foodName, record.get(FieldNames.brands),
                    CATEGORIES_BLACKLIST, MAX_LENGTH_BRAND_NAME, MAX_NUM_BRANDS)

            val categoriees = Utils.buildUniqueList(foodName, record.get(FieldNames.categories_en) + "," + record.get(FieldNames.generic_name),
                    CATEGORIES_BLACKLIST, MAX_LENGTH_CATEGORY_NAME, MAX_NUM_CATEGORIES)
            food.categories = categoriees
            food.isBeverage = Utils.isBeverage(record.get(FieldNames.quantity))

            try {
                val kiloJoule = record.get(FieldNames.energy_100g).toInt()
                food.calories = Math.round(kiloJoule * KILOJOULE_TO_KCAL_FACTOR).toInt()
                food.weight = 100.0f
                food.protein = record.get(FieldNames.proteins_100g).toFloat()
                food.carbohydrate = record.get(FieldNames.carbohydrates_100g).toFloat()
                food.fat = record.get(FieldNames.fat_100g).toFloat()
            } catch (e: NumberFormatException) {
                continue
            }

            val portionSet = Utils.getPortions(record.get(FieldNames.serving_size), PORTION_TRANSLATION[index])
            food.portions = portionSet

            foodNameBrandMap.put(food.name + food.brands, food)
            foodCountAfterFiltering++
        }

        println(countryName.toUpperCase())
        println("Longest food name: $longestFoodName ($maxLength characters)")

        val uniqueFilteredFoods = foodNameBrandMap.values.toMutableList()
        println("Number of foods overall = " + foodCountForCountry)
        println(String.format("Number of foods after filtering = %d (%d are unique)", foodCountAfterFiltering, uniqueFilteredFoods.size))

        val sorted = uniqueFilteredFoods.sortedWith(compareBy({ it.name }))

        try {
            println("Dump size: " + Utils.writeToFile(sorted, countryName) + " kB")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        println("____________________________________________________________")

        index++
    }

    println(numErrors.toString() + " lines were not formatted properly!")
}