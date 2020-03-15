package io.github.erfangc.assets.parser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceStockAssetParser
import org.junit.jupiter.api.Test

internal class YFinanceStockAssetParserTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun parseTicker() {
        YFinanceStockAssetParser(objectMapper).parseTicker("AAPL")
        YFinanceStockAssetParser(objectMapper).parseTicker("C")
        YFinanceStockAssetParser(objectMapper).parseTicker("HAS")
        YFinanceStockAssetParser(objectMapper).parseTicker("BA")
    }
}