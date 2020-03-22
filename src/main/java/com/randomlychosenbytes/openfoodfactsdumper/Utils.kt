package com.randomlychosenbytes.openfoodfactsdumper

import java.io.File
import java.util.regex.Pattern
import kotlin.system.measureTimeMillis

const val KILOJOULE_TO_KCAL_FACTOR = 0.239006f

val countryNameToPortionTranslationMap = mapOf(
        "Australia" to "portion",
        "Belgium" to "deel",
        "Canada" to "portion",
        "France" to "portion",
        "Germany" to "Portion",
        "Italy" to "porzione",
        "Netherlands" to "deel",
        "Spain" to "parte",
        "Switzerland" to "Portion",
        "United Kingdom" to "portion",
        "United States" to "portion"
)

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

fun writeFoodListToFile(foods: Sequence<Food>, destinationFile: File) {
    destinationFile.writer().use { writer ->
        foods.forEach {
            writer.write("${it.toCsvLine()}\n")
        }
    }
}

val anyNumberRegex = Pattern.compile("""\d*[,.]?\d+""")

fun getFirstNumberInString(str: String): Float? {

    val m = anyNumberRegex.matcher(str)

    if (!m.find()) {
        return null
    }

    val portionWeight = m.group().replace(",", ".").toFloatOrNull() ?: return null

    if (portionWeight == 0f || portionWeight == 1f || portionWeight == 100f) {
        return null
    }

    return portionWeight
}

fun isBeverage(quantityStr: String) = with(quantityStr.toLowerCase()) {
    listOf("litre", "l", "ml", "cl").any { it in this }
}

fun <T> timeIt(message: String, block: () -> T) {
    println("$message started")
    val seconds = (measureTimeMillis { block() } / 1000.0).toInt()
    println("$message finished (took $seconds s)")
}
