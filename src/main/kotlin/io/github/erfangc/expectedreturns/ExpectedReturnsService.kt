package io.github.erfangc.expectedreturns

import io.github.erfangc.assets.AssetTimeSeriesService
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@Service
class ExpectedReturnsService(private val assetTimeSeriesService: AssetTimeSeriesService) {

    internal data class FactorLevel(val name: String, val date: LocalDate, val value: Double)

    private val formatter = DateTimeFormatterBuilder()
            .appendPattern("yyyyMM")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter()

    /**
     * Statically load the French Fama factors so we can quickly regress them
     * against monthly returns
     */
    private val factorLevels = ClassPathResource("french-fama-3-factors.csv")
            .inputStream
            .bufferedReader()
            .lineSequence()
            .drop(1)
            .toList()
            .map { line ->
                val parts = line.split(",")
                val date = LocalDate.parse(parts[0], formatter)
                val market = parts[1].trim().toDouble() / 100.0
                val smb = parts[2].trim().toDouble() / 100.0
                val hml = parts[3].trim().toDouble() / 100.0
                val rf = parts[4].trim().toDouble() / 100.0
                date to mapOf(
                        "market" to FactorLevel(
                                name = "market",
                                date = date,
                                value = market
                        ),
                        "smb" to FactorLevel(
                                name = "smb",
                                date = date,
                                value = smb
                        ),
                        "hml" to FactorLevel(
                                name = "hml",
                                date = date,
                                value = hml
                        ),
                        "rf" to FactorLevel(
                                name = "rf",
                                date = date,
                                value = rf
                        )
                )
            }
            .toMap()

    /**
     * Compute the expected returns of assets given their id
     *
     * @return a Map whose keys are assetIds and the values are expected returns for the asset
     */
    fun getExpectedReturns(assetIds: List<String>): Map<String, Double> {

        // first find the monthly return time series for our asset
        // we take the past 5 years or 60 observations, filter out dates where all
        // returns were zero

        // set to final date to the most recent month end
        val now = LocalDate.now()
        val lastMonth = now.minusMonths(1)
        // this should be the last day of last month
        val stop = lastMonth.minusDays(lastMonth.dayOfMonth.toLong() - 1)
        val start = stop.minusYears(5)

        // associate the return time series by assetId then by date
        val monthlySeries = assetTimeSeriesService
                .getMonthlyReturnTimeSeries(assetIds, start, stop)
                .groupBy {
                    it.assetId
                }
                .mapValues { entry ->
                    entry.value.associateBy { datum -> datum.date }
                }

        val months = months(start, stop)

        // create the x for the multi-variable regression
        val x = months.map { month ->
            val factorLevel = factorLevels[month] ?: throw IllegalStateException()
            doubleArrayOf(
                    factorLevel["market"]?.value ?: 0.0,
                    factorLevel["smb"]?.value ?: 0.0,
                    factorLevel["hml"]?.value ?: 0.0
            )
        }.toTypedArray()

        // find the averages
        val averages = doubleArrayOf(
                x.map { it[0] }.average(),
                x.map { it[1] }.average(),
                x.map { it[2] }.average()
        )

        // create the y(s)
        return assetIds.map { assetId ->
            val monthlyReturns = monthlySeries[assetId] ?: throw IllegalStateException()
            val y = months.map { monthlyReturns[it]?.value ?: 0.0 }.toDoubleArray()
            val ols = OLSMultipleLinearRegression()
            ols.newSampleData(y, x)
            val betas = ols.estimateRegressionParameters()
            val mu = averages.mapIndexed { idx, average -> betas[idx + 1] * average }.sum()
            // expected returns must be annualized
            assetId to mu * 12
        }.toMap()
    }

    // determine the set of months between start -> stop by adding a month until stop
    private fun months(start: LocalDate, stop: LocalDate): List<LocalDate> {
        var currentMonth = start
        val months = mutableListOf<LocalDate>()
        while (currentMonth.isBefore(stop)) {
            months.add(currentMonth)
            currentMonth = currentMonth.plusMonths(1)
        }
        return months
    }
}