package io.github.erfangc.assets

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AssetTimeSeriesServiceTest {
    @Test
    internal fun getMonthlyReturnSeries() {
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val svc = AssetTimeSeriesService(ddb)
        val timeSeries = svc.getMonthlyReturnTimeSeries(listOf("IVV","AGG"), LocalDate.of(2015, 1, 1), LocalDate.now())
        timeSeries.size
    }
}