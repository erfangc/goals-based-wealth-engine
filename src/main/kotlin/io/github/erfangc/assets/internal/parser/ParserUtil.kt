package io.github.erfangc.assets.internal.parser

import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import us.codecraft.xsoup.Xsoup
import java.text.NumberFormat

object ParserUtil {
    fun parsePreviousClose(summary: Document?): Double? {
        val trs = Xsoup
                .compile("//*[@id=\"quote-summary\"]/div[1]/table/tbody/tr")
                .evaluate(summary)
                .elements
        val cellToFind = "Previous Close"
        return searchTableRows(trs, cellToFind)
    }

    fun parseDouble(value: Any): Double? {
        return try {
            val valueAsString = value.toString().trim()
            val multiplier = when (valueAsString.last()) {
                'M' -> 1000000
                'B' -> 1000000000
                else -> 1
            }
            NumberFormat.getNumberInstance(java.util.Locale.US).parse(valueAsString).toDouble() * multiplier
        } catch (e: Exception) {
            null
        }
    }

    fun searchTableRows(trs: Elements, cellToFind: String): Double? {
        return trs
                .find { tr ->
                    tr.select("td").first().text() == cellToFind
                }
                ?.select("td")
                ?.last()
                ?.text()
                ?.let { parseDouble(it) }
    }

    fun parsePercentage(value: Any?): Double {
        return value?.toString()?.replace("%", "")?.toDoubleOrNull()?:0.0
    }
}