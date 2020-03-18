package com.randomlychosenbytes.openfoodfactsdumper

import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.math.roundToInt

/**
 * OpenFoodFactsDumpCreator 0.1 Alpha
 * Query open food facts database for countries and the number of food available for them
 * SELECT countries_en, COUNT(*) as count from foods GROUP BY countries_en ORDER BY count DESC

 * @author wmentzel
 */

const val KILOJOULE_TO_KCAL_FACTOR = 0.239006f
const val FILE_IMPORT_PATH = "/home/willi/Downloads/off"
val countries = arrayOf("Belgium", "United States", "Germany", "France", "Spain", "United Kingdom", "Switzerland", "Australia", "Italy")
val portionTranslations = arrayOf("deel", "portion", "Portion", "portion", "parte", "portion", "Portion", "portion", "porzione")
val countryNameToPortionTranslation = countries.zip(portionTranslations).toMap()

//
// Constraints
//
const val MAX_NUM_CATEGORIES = 3
const val MAX_LENGTH_CATEGORY_NAME = 64
const val MAX_LENGTH_FOOD_NAME = 64
const val MAX_LENGTH_BRAND_NAME = 64
const val MAX_NUM_BRANDS = 3
val CATEGORIES_BLACKLIST = arrayOf("fr:", "en:", "de:", "//")

var columnToIndex: Map<String, Int>? = null

fun main() {
    val countryFiles = countries.associate { it to File("$FILE_IMPORT_PATH/$it") }

    // clear old files
    countryFiles.values.filter { it.exists() }.forEach { it.delete() }

    timeIt("Partition big file into country specific smaller ones") {
        File("$FILE_IMPORT_PATH/en.openfoodfacts.org.products.csv").forEachLine { line ->

            if (columnToIndex == null) {
                columnToIndex = line.split('\t').withIndex().associate { it.value to it.index }
            }

            val countryName = line.split('\t')[columnToIndex!!.getValue("countries_en")]
            if (countryName in countries) {
                countryFiles.getValue(countryName).appendText("$line\n")
            }
        }
    }

    timeIt("Create a CC compatible file for each country") {
        countries.forEach { countryName ->
            timeIt("for $countryName") {
                val csvRecords = LinkedList<CSVRecord>()

                FileReader("$FILE_IMPORT_PATH/$countryName").forEachCsvRecord {
                    csvRecords.add(it)
                }

                val foods = getFoodListFromCSVRecords(csvRecords, countryNameToPortionTranslation.getValue(countryName))

                val destinationFile = File("$FILE_IMPORT_PATH/db_dump_${countryName.replace(' ', '_').toLowerCase()}.csv")
                destinationFile.delete()
                writeFoodListToFile(foods, destinationFile)
            }
        }
    }
}

fun getFoodListFromCSVRecords(csvRecords: List<CSVRecord>, portionTranslation: String): List<Food> {
    var foodCountAfterFiltering = 0
    var foodCountForCountry = 0

    var maxLength = 0
    var longestFoodName = ""

    val foodNameBrandMap = HashMap<String, Food>()

    for (record in csvRecords) {

        if (record.size() < 159) { // is the line invalid?
            continue
        }

        foodCountForCountry++

        //
        // Create the food object
        //
        val food = Food()

        val foodName = record.get(columnToIndex!!.getValue("product_name")).trim().replace("&quot;", "")

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
        food.brands = buildUniqueList(foodName, record.get(columnToIndex!!.getValue("brands")),
                CATEGORIES_BLACKLIST, MAX_LENGTH_BRAND_NAME, MAX_NUM_BRANDS)

        val categoriees = buildUniqueList(foodName, record.get(columnToIndex!!.getValue("categories_en")) + "," + record.get(columnToIndex!!.getValue("generic_name")),
                CATEGORIES_BLACKLIST, MAX_LENGTH_CATEGORY_NAME, MAX_NUM_CATEGORIES)
        food.categories = categoriees
        food.isBeverage = isBeverage(record.get(columnToIndex!!.getValue("quantity")))

        try {
            val kiloJoule = record.get(columnToIndex!!.getValue("energy_100g")).toInt()
            food.calories = (kiloJoule * KILOJOULE_TO_KCAL_FACTOR).roundToInt()
            food.weight = 100.0f
            food.protein = record.get(columnToIndex!!.getValue("proteins_100g")).toFloat()
            food.carbohydrate = record.get(columnToIndex!!.getValue("carbohydrates_100g")).toFloat()
            food.fat = record.get(columnToIndex!!.getValue("fat_100g")).toFloat()
        } catch (e: NumberFormatException) {
            continue
        }

        food.portions = getFirstNumberInString(record.get(columnToIndex!!.getValue("serving_size")))?.let {
            setOf(Portion(portionTranslation, it))
        } ?: setOf()

        foodNameBrandMap[food.name + food.brands] = food
        foodCountAfterFiltering++
    }


    println("Longest food name: $longestFoodName ($maxLength characters)")

    val uniqueFilteredFoods = foodNameBrandMap.values
    println("Number of foods overall = $foodCountForCountry")
    println(String.format("Number of foods after filtering = %d (%d are unique)", foodCountAfterFiltering, uniqueFilteredFoods.size))

    return uniqueFilteredFoods.sortedBy { it.name }
}

