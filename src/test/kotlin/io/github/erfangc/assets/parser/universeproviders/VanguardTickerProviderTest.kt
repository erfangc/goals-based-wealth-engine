package io.github.erfangc.assets.parser.universeproviders

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceFundAssetParser
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class VanguardTickerProviderTest {

    @Test
    fun run() {
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val svc = VanguardTickerProvider(
                yFinanceFundAssetParser = YFinanceFundAssetParser(ddb = ddb),
                yFinanceTimeSeriesDownloader = YFinanceTimeSeriesDownloader(
                        httpClient = HttpClientBuilder.create().build(),
                        ddb = ddb,
                        objectMapper = objectMapper
                )
        )
        svc.run()
    }
}