package io.github.erfangc.expectedreturns

import io.github.erfangc.assets.AssetService
import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.util.DateUtils.months
import io.github.erfangc.util.DateUtils.mostRecentMonthEnd
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@Service
class ExpectedReturnsService(
        private val assetTimeSeriesService: AssetTimeSeriesService,
        private val assetService: AssetService
) {

    internal data class FactorLevel(val name: String, val date: LocalDate, val value: Double)

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendPattern("yyyyMM")
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter()!!
    }

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
                        // basically broad equity market performance
                        "market" to FactorLevel(
                                name = "market",
                                date = date,
                                value = market
                        ),
                        // small minus big
                        "smb" to FactorLevel(
                                name = "smb",
                                date = date,
                                value = smb
                        ),
                        // high minus low
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
     * Compute factor premium looking back arbitrarily long
     */
    private fun factorPremiums(): Map<String, Double> {
        val stop = mostRecentMonthEnd()
        // we take 30 years of history to compute factor premiums. long histories are needed
        // to avoid short-term fluctuations and instabilities
        val months = months(stop.minusYears(30), stop)
        val totals = months
                .fold(mapOf("market" to 0.0, "smb" to 0.0, "hml" to 0.0)) { acc, date ->
                    mapOf(
                            "market" to (acc["market"] ?: error("")) + (factorLevels[date]?.get("market")?.value
                                    ?: error("")),
                            "smb" to (acc["smb"] ?: error("")) + (factorLevels[date]?.get("smb")?.value ?: error("")),
                            "hml" to (acc["hml"] ?: error("")) + (factorLevels[date]?.get("hml")?.value ?: error(""))
                    )
                }
        // take an average by dividing by the number of observations
        return totals.mapValues { it.value / months.size }
    }

    /**
     * Compute the expected returns of assets given their id
     *
     * @return a Map whose keys are assetIds and the values are expected returns for the asset
     */
    fun getExpectedReturns(assetIds: List<String>): Map<String, ExpectedReturn> {

        // first find the monthly return time series for our asset
        // we take the past 5 years or 60 observations, filter out dates where all
        // returns were zero

        // set to final date to the most recent month end
        val lastMonth = mostRecentMonthEnd()
        // this should be the last day of last month
        val start = lastMonth.minusYears(5)

        // associate the return time series by assetId then by date
        val monthlySeries = assetTimeSeriesService
                .getMonthlyReturnTimeSeries(assetIds, start, lastMonth)
                .groupBy {
                    it.assetId
                }
                .mapValues { entry ->
                    entry.value.associateBy { datum -> datum.date }
                }

        val months = months(start, lastMonth)

        // create the x for the multi-variable regression
        val x = months.map { month ->
            val factorLevel = factorLevels[month] ?: throw IllegalStateException()
            doubleArrayOf(
                    factorLevel["market"]?.value ?: error(""),
                    factorLevel["smb"]?.value ?: error(""),
                    factorLevel["hml"]?.value ?: error("")
            )
        }.toTypedArray()

        // find the averages
        val factorPremiums = factorPremiums()
        val averages = doubleArrayOf(
                factorPremiums["market"] ?: error(""),
                factorPremiums["smb"] ?: error(""),
                factorPremiums["hml"] ?: error("")
        )

        val assets = assetService.getAssets(assetIds).associateBy { it.id }
        // create the y(s)
        return assetIds.filter { it != "USD" }.map { assetId ->
            val asset = assets[assetId]
            if (asset?.assetClass == "Bond") {
                val `yield` = asset.yield?.div(100.0) ?: 0.0
                assetId to (ExpectedReturn(expectedReturn = `yield`, `yield` = `yield`))
            } else {
                val monthlyReturns = monthlySeries[assetId]
                        ?: throw IllegalStateException("cannot find monthly returns for $assetId")
                val y = months.map { date -> monthlyReturns[date.toString()]?.value ?: 0.0 }.toDoubleArray()
                val ols = OLSMultipleLinearRegression()
                ols.newSampleData(y, x)
                val betas = ols.estimateRegressionParameters()
                // note the idx + 1, OLS estimate parameters makes the 1st parameter the intercept
                val mu = averages.mapIndexed { idx, average -> betas[idx + 1] * average }.sum()
                // expected returns must be annualized
                assetId to ExpectedReturn(expectedReturn = mu * 12, marketSensitivity = betas[1], smb = betas[2], hml = betas[3])
            }
        }.toMap() + ("USD" to ExpectedReturn(expectedReturn = 0.0))
    }

}