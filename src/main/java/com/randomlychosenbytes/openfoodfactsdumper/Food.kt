package com.randomlychosenbytes.openfoodfactsdumper

data class Food(
        var name: String, var brands: String, var categories: String, var weight: Float, var calories: Int,
        var fat: Float, var carbohydrate: Float, var protein: Float, var isBeverage: Boolean, var portions: Set<Portion>
) {
    constructor() : this(
            "",
            "",
            "",
            0f,
            0,
            0f,
            0f,
            0f,
            false,
            setOf()
    )

    fun toCsvLine(): String {
        val fat = fat.toString().replace(",", ".")
        val carbohydrate = carbohydrate.toString().replace(",", ".")
        val protein = protein.toString().replace(",", ".")

        val portionsStr = portions.toString().removeSurrounding("[", "]")

        // add portions
        return "$name|$brands|$categories|$weight|$calories|$fat|$carbohydrate|$protein|$isBeverage|{$portionsStr}"
    }
}
