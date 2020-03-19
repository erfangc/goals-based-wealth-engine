package io.github.erfangc.assets.parser.universeproviders

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceStockAssetParser
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class StockTickerProviderTest {

    @Test
    fun run() {
        val httpClient =  HttpClientBuilder.create().build()
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val svc = StockTickerProvider(
                yFinanceTimeSeriesDownloader = YFinanceTimeSeriesDownloader(
                        httpClient = httpClient,
                        ddb = ddb,
                        objectMapper = objectMapper
                ),
                yFinanceStockAssetParser = YFinanceStockAssetParser(
                        objectMapper = objectMapper,
                        ddb = ddb
                )
        )
        svc.run()
    }
}