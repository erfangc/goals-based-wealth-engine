package io.github.erfangc.assets.parser

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

internal class TimeSeriesParserTest {

    @Test
    fun downloadHistoryForTicker() {

        val httpClient = HttpClientBuilder.create().build()
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()

        val svc = TimeSeriesParser(httpClient = httpClient, objectMapper = objectMapper, ddb = ddb)

        svc.downloadHistoryForTicker("AGG", true)
        svc.downloadHistoryForTicker("IVV", true)
        svc.downloadHistoryForTicker("VTI", true)
        svc.downloadHistoryForTicker("VEA", true)
        svc.downloadHistoryForTicker("VWO", true)
        svc.downloadHistoryForTicker("VXF", true)
        svc.downloadHistoryForTicker("BND", true)
        svc.downloadHistoryForTicker("BNDX", true)
        svc.downloadHistoryForTicker("C", true)
        svc.downloadHistoryForTicker("AAPL", true)
        svc.downloadHistoryForTicker("AMZN", true)

    }
}