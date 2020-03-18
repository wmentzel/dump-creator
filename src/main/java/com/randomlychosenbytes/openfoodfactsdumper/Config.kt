package com.randomlychosenbytes.openfoodfactsdumper

const val FILE_IMPORT_PATH = "/home/willi/Downloads/off"

val countries = arrayOf(
        "Belgium",
        "United States",
        "Germany",
        "France",
        "Spain",
        "United Kingdom",
        "Switzerland",
        "Australia",
        "Italy"
)

val portionTranslations = arrayOf(
        "deel",
        "portion",
        "Portion",
        "portion",
        "parte",
        "portion",
        "Portion",
        "portion",
        "porzione"
)

//
// Constraints
//
const val MAX_NUM_CATEGORIES = 3
const val MAX_LENGTH_CATEGORY_NAME = 64
const val MAX_LENGTH_FOOD_NAME = 64
const val MAX_LENGTH_BRAND_NAME = 64
const val MAX_NUM_BRANDS = 3
val CATEGORIES_BLACKLIST = arrayOf("fr:", "en:", "de:", "//")
