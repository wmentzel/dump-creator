package com.randomlychosenbytes.openfoodfactsdumper

import java.io.*
import java.util.*
import java.util.regex.Pattern

object Utils {

    val NUMBER_PATTERN = Pattern.compile("-?\\d+")

    fun buildUniqueList(ignore: String, filterString: String, blacklistArray: Array<String>, maxElementLength: Int, maxNumElements: Int): String {
        val filterArray = filterString.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

        // trim
        for (i in filterArray.indices) {
            filterArray[i] = filterArray[i].trim({ it <= ' ' })
        }

        // unique
        val set = HashSet<String>()
        set.addAll(Arrays.asList<String>(*filterArray))

        val output = StringBuilder()

        for ((i, name) in set.withIndex()) {

            if (i == maxNumElements) { // three cateogies are enough
                break
            }

            val str = name.trim()
            val isNotTheFoodName = str != ignore
            val isNotOnlyWhiteSpaces = str.replace(" ".toRegex(), "").length != 0
            val isNotEmpty = !str.isEmpty()
            val isLengthOk = str.length <= maxElementLength
            val isNotOnIgnoreList = !isStringContainedInListElement(str, blacklistArray)

            if (isNotTheFoodName && isNotEmpty
                    && isLengthOk && isNotOnlyWhiteSpaces
                    && isNotOnIgnoreList) {

                if (i != 0) {
                    output.append(", ")
                }

                output.append(str)
            }
        }

        return output.toString()
    }

    fun isStringContainedInListElement(subject: String, list: Array<String>): Boolean {
        list.forEach {
            if (subject.contains(it)) {
                return true
            }
        }

        return false
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun writeToFile(foods: List<Food>, countryName: String): Long {
        val FILE_EXPORT_PATH = "new/db_dump_" + countryName.replace(' ', '_').toLowerCase() + ".csv"

        val fOut = FileOutputStream(FILE_EXPORT_PATH)
        val osw = OutputStreamWriter(fOut, "UTF-8")

        foods.forEach {
            osw.write(it.toCsvLine() + "\n") // {} is a portion json string
        }

        osw.flush()
        osw.close()

        val fileSizeBytes = File(FILE_EXPORT_PATH).length()
        return (fileSizeBytes / 1000.0).toLong()
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
                portionsSet.add(Portion("1 " + portionTranslation, portionWeight))
            } catch (e: NumberFormatException) {
                // don't add it
            }

            break
        }

        return portionsSet
    }

    fun isBeverage(quantityStr: String): Boolean {
        var quantityStr = quantityStr.toLowerCase()
        return quantityStr.contains("litre") || quantityStr.contains("l") || quantityStr.contains("ml") || quantityStr.contains("cl")
    }

    @Throws(NumberFormatException::class)
    fun getWeightInGrams(quantityStr: String): Float {
        var quantityStr = quantityStr.toLowerCase()

        if (quantityStr.isEmpty()) {
            return 100f
        }

        if (quantityStr.contains("kg") || quantityStr.contains("l")) {
            return quantityStr.replace("kg", "").toFloat() * 1000f
        }

        if (quantityStr.contains("l")) {
            return quantityStr.replace("l", "").toFloat() * 1000f
        }

        if (quantityStr.contains("g")) {
            return quantityStr.replace("g", "").toFloat() * 1f
        }

        if (quantityStr.contains("ml")) {
            return quantityStr.replace("ml", "").toFloat() * 1f
        }

        if (quantityStr.contains("cl")) {
            return quantityStr.replace("cl", "").toFloat() * 10f
        }

        return 100f
    }
}
