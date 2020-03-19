package io.github.erfangc.assets.parser

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceStockAssetParser
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class YFinanceStockAssetParserTest {

    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()

    @Test
    fun parseTicker() {
        val ddb = mockk<AmazonDynamoDB>()
        YFinanceStockAssetParser(objectMapper, ddb).parseTicker("GLADD")
    }
}