package io.github.erfangc.assets.parser.universeproviders

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceFundAssetParser
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ISharesTickerProviderTest {

    @Test
    fun run() {
        val httpClient = HttpClientBuilder.create().build()
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val yFinanceFundAssetParser = YFinanceFundAssetParser(ddb = ddb)
        val yFinanceTimeSeriesDownloader = YFinanceTimeSeriesDownloader(
                httpClient = httpClient,
                objectMapper = objectMapper,
                ddb = ddb
        )
        val svc = ISharesTickerProvider(
                httpClient = httpClient,
                objectMapper = objectMapper,
                yFinanceTimeSeriesDownloader = yFinanceTimeSeriesDownloader,
                yFinanceFundAssetParser = yFinanceFundAssetParser
        )
        svc.run()
    }

}