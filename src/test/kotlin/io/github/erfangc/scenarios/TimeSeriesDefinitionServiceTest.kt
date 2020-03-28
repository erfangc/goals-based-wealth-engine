package io.github.erfangc.scenarios

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

internal class TimeSeriesDefinitionServiceTest {

    @Test
    fun downloadTimeSeries() {
        val httpClient = HttpClientBuilder.create().build()
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()

        val yFinanceTimeSeriesDownloader = YFinanceTimeSeriesDownloader(
                httpClient = httpClient,
                objectMapper = objectMapper,
                ddb = ddb
        )
        val svc = TimeSeriesDefinitionService(
                yFinanceTimeSeriesDownloader = yFinanceTimeSeriesDownloader
        )
        svc.downloadTimeSeries()
    }

}