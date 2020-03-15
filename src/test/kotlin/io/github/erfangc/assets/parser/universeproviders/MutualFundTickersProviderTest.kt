package io.github.erfangc.assets.parser.universeproviders

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceFundAssetParser
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesParser
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

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
                yFinanceTimeSeriesParser = YFinanceTimeSeriesParser(httpClient, objectMapper, ddb)
        )
        svc.run()
    }
}