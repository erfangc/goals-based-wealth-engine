package io.github.erfangc.assets.parser

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

internal class YFinanceTimeSeriesDownloaderTest {

    @Test
    fun downloadHistoryForTicker() {

        val httpClient = HttpClientBuilder.create().build()
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()

        val svc = YFinanceTimeSeriesDownloader(httpClient = httpClient, objectMapper = objectMapper, ddb = ddb)

        svc.downloadHistoryForTicker("AGG", save = true)
        svc.downloadHistoryForTicker("IVV", save = true)
        svc.downloadHistoryForTicker("VTI", save = true)
        svc.downloadHistoryForTicker("VEA", save = true)
        svc.downloadHistoryForTicker("VWO", save = true)
        svc.downloadHistoryForTicker("VXF", save = true)
        svc.downloadHistoryForTicker("BND", save = true)
        svc.downloadHistoryForTicker("BNDX", save = true)
        svc.downloadHistoryForTicker("C", save = true)
        svc.downloadHistoryForTicker("AAPL", save = true)
        svc.downloadHistoryForTicker("AMZN", save = true)

    }
}