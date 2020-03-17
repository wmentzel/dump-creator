package com.randomlychosenbytes.openfoodfactsdumper

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

object Utils {

    val NUMBER_PATTERN = Pattern.compile("-?\\d+")

    fun buildUniqueList(
            ignore: String,
            str: String,
            blacklistArray: Array<String>,
            maxElementLength: Int,
            maxNumElements: Int
    ) = str
            .split(",")
            .asSequence()
            .filter(String::isEmpty)
            .map(String::trim)
            .distinct()
            .take(maxNumElements).filter { name ->
                val str = name.trim()
                val isNotTheFoodName = str != ignore
                val isNotOnlyWhiteSpaces = str.any { it != ' ' }
                val isNotEmpty = str.isNotEmpty()
                val isLengthOk = str.length <= maxElementLength
                val isNotOnIgnoreList = blacklistArray.none { it == str }

                isNotTheFoodName && isNotEmpty && isLengthOk && isNotOnlyWhiteSpaces && isNotOnIgnoreList
            }.joinToString()

    @Throws(FileNotFoundException::class, IOException::class)
    fun writeToFile(foods: List<Food>, countryName: String): Long {

        val f = File("new/db_dump_${countryName.replace(' ', '_').toLowerCase()}.csv")
        f.writer().use { writer ->
            foods.forEach {
                writer.write(it.toCsvLine() + "\n") // {} is a portion json string
            }
        }

        return (f.length() / 1000.0).toLong()
    }

    fun getPortions(str: String, portionTranslation: String): Set<Portion> {
        val portionsSet = HashSet<Portion>()

        val m = NUMBER_PATTERN.matcher(str)
        while (m.find()) {
            try {
                val portionWeight = m.group().toFloat()

                if (portionWeight == 0f || portionWeight == 1f || portionWeight == 100f) {
                    break
                }
                portionsSet.add(Portion("1 $portionTranslation", portionWeight))
            } catch (e: NumberFormatException) {
                // don't add it
            }

            break
        }

        return portionsSet
    }

    fun isBeverage(quantityStr: String) = with(quantityStr.toLowerCase()) {
        listOf("litre", "l", "ml", "cl").any { it in this }
    }
}
