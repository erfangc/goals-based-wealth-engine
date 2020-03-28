package io.github.erfangc.scenarios

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.util.DriverDataSource
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*
import javax.sql.DataSource

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
        val jdbcTemplate = NamedParameterJdbcTemplate(dataSource())
        val svc = TimeSeriesDefinitionService(
                jdbcTemplate = jdbcTemplate,
                yFinanceTimeSeriesDownloader = yFinanceTimeSeriesDownloader
        )
        svc.init()
        svc.downloadTimeSeries()
    }

    private fun dataSource(): DataSource {
        val url = "jdbc:postgresql://localhost:5432/erfangchen"
        val username = "postgres"
        val password = ""
        return DriverDataSource(
                url,
                "org.postgresql.Driver",
                Properties(),
                username,
                password
        )
    }
}