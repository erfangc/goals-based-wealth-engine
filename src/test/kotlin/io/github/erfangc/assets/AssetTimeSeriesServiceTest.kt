package io.github.erfangc.assets

import org.junit.jupiter.api.Test

import java.time.LocalDate

internal class AssetTimeSeriesServiceTest {

    @Test
    fun getDailyReturnTimeSeries() {
        val svc = AssetTimeSeriesService()
        val assetIds = listOf("BND")
        val start = LocalDate.of(2010, 1, 15)
        val stop = LocalDate.of(2018, 5, 2)
        val dailyReturnTimeSeries = svc.getDailyReturnTimeSeries(assetIds, start, stop)
        dailyReturnTimeSeries.forEach {
            println("${it.date},${it.value}")
        }
    }

    @Test
    fun getMonthlyReturnTimeSeries() {
        val svc = AssetTimeSeriesService()
        val assetIds = listOf("VEA")
        val start = LocalDate.of(2010, 1, 15)
        val stop = LocalDate.of(2018, 5, 2)
        val returnTimeSeries = svc.getMonthlyReturnTimeSeries(assetIds, start, stop)
        returnTimeSeries.forEach {
            println("${it.date},${it.value}")
        }
    }
}