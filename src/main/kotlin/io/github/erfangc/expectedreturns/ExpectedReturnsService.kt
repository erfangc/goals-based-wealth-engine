package io.github.erfangc.expectedreturns

import io.github.erfangc.assets.AssetService
import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.common.DateUtils.months
import io.github.erfangc.common.DateUtils.mostRecentMonthEnd
import io.github.erfangc.expectedreturns.internal.FrenchFamaFactorLevel
import io.github.erfangc.expectedreturns.internal.FrenchFamaFactorsParser
import io.github.erfangc.expectedreturns.models.ExpectedReturn
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@Service
class ExpectedReturnsService(
        private val assetTimeSeriesService: AssetTimeSeriesService,
        private val assetService: AssetService,
        frenchFamaFactorsParser: FrenchFamaFactorsParser
) {

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendPattern("yyyyMM")
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter()!!
    }

    val factorLevels: Map<LocalDate, FrenchFamaFactorLevel> = frenchFamaFactorsParser.factorLevels()

    internal data class FactorPremiums(
            val mktMinusRf: Double = 0.0,
            val smb: Double = 0.0,
            val hml: Double = 0.0,
            val rf: Double = 0.0
    )

    /**
     * Compute factor premium looking back arbitrarily long
     */
    private fun factorPremiums(): FactorPremiums {
        val stop = mostRecentMonthEnd()
        // we take 30 years of history to compute factor premiums. long histories are needed
        // to avoid short-term fluctuations and instabilities
        val months = months(stop.minusYears(30), stop)

        // take an average by dividing by the number of observations
        return months.fold(FactorPremiums()) { acc, date ->
            val mktMinusRf = factorLevels[date]?.mktMinusRf ?: 0.0
            val rf = factorLevels[date]?.rf ?: 0.0
            val smb = factorLevels[date]?.smb ?: 0.0
            val hml = factorLevels[date]?.hml ?: 0.0
            acc.copy(
                    mktMinusRf = (acc.mktMinusRf + mktMinusRf / months.size),
                    rf = (acc.rf + rf / months.size),
                    smb = (acc.smb + smb / months.size),
                    hml = (acc.hml + hml / months.size)
            )
        }
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
                    factorLevel.mktMinusRf,
                    factorLevel.smb,
                    factorLevel.hml
            )
        }.toTypedArray()

        // find the averages
        val factorPremiums = factorPremiums()
        val averages = doubleArrayOf(
                factorPremiums.mktMinusRf,
                factorPremiums.smb,
                factorPremiums.hml
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