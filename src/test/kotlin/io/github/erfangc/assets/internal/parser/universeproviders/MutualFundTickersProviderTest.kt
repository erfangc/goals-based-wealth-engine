package io.github.erfangc.assets.internal.parser.universeproviders

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.yfinance.YFinanceFundAssetParser
import io.github.erfangc.assets.yfinance.YFinanceTimeSeriesDownloader
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

internal class MutualFundTickersProviderTest {

    @Test
    fun run() {
        val httpClient = HttpClientBuilder.create().build()
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val svc = MutualFundTickersProvider(
                httpClient = httpClient,
                objectMapper = objectMapper,
                yFinanceFundAssetParser = YFinanceFundAssetParser(ddb = ddb),
                yFinanceTimeSeriesDownloader = YFinanceTimeSeriesDownloader(httpClient, objectMapper, ddb)
        )
        svc.run()
    }
}