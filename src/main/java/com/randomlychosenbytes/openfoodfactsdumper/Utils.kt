package com.randomlychosenbytes.openfoodfactsdumper

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileReader
import java.util.regex.Pattern
import kotlin.system.measureTimeMillis

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

fun writeFoodListToFile(foods: List<Food>, destinationFile: File) {
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
    println("$message finished (took $seconds)")
}

internal fun FileReader.forEachCsvRecord(operation: (CSVRecord) -> Unit) {
    use { fileReader ->
        val csvFileFormat = CSVFormat.DEFAULT.withSkipHeaderRecord().withDelimiter('\t')
        val csvFileParser = CSVParser(fileReader, csvFileFormat)

        val iterator = csvFileParser.iterator()

        while (true) {
            try {
                if (iterator.hasNext()) { // even hasNext() throws
                    operation(iterator.next())
                } else {
                    break
                }
            } catch (e: Exception) {
            }
        }
    }
}
