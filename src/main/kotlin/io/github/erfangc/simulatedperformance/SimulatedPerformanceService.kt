package io.github.erfangc.simulatedperformance

import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.assets.TimeSeriesDatum
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.simulatedperformance.models.*
import io.github.erfangc.util.DateUtils
import io.github.erfangc.util.DateUtils.months
import io.github.erfangc.util.PortfolioUtils.assetIds
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SimulatedPerformanceService(
        private val assetTimeSeriesService: AssetTimeSeriesService,
        private val marketValueAnalysisService: MarketValueAnalysisService
) {

    internal data class Context(
            val weights: Map<String, Map<String, Double>>,
            val assetReturns: Map<LocalDate, Map<String, TimeSeriesDatum>>,
            val months: List<LocalDate>,
            val request: SimulatedPerformanceRequest
    )

    fun maximumDrawdown(timeSeries: List<TimeSeries>): MaximumDrawdown {
        var maximumDrawdown = 0.0
        var currentPeak = timeSeries.first().cumulativeReturn - 1.0
        var currentPeakDate = timeSeries.first().date
        var start = currentPeakDate
        var stop = currentPeakDate
        for (datum in timeSeries) {
            val cumulativeReturn = datum.cumulativeReturn - 1.0
            val date = datum.date
            if (cumulativeReturn > currentPeak) {
                currentPeak = cumulativeReturn
                currentPeakDate = date
            }

            val drawdown = currentPeak - cumulativeReturn
            if (drawdown > maximumDrawdown) {
                maximumDrawdown = drawdown
                start = currentPeakDate
                stop = date
            }
        }
        return MaximumDrawdown(value = maximumDrawdown, start = start, stop = stop)
    }

    fun analyze(request: SimulatedPerformanceRequest): SimulatedPerformanceResponse {

        val weights = weights(request)
        val stop = DateUtils.mostRecentMonthEnd()
        val start = stop.minusYears(30)
        val assetIds = assetIds(request.portfolios)
        val assetReturns = assetReturns(assetIds, start, stop)

        val minDate = assetReturns.keys.min() ?: error("")
        val maxDate = assetReturns.keys.max() ?: error("")

        val ctx = Context(
                weights = weights,
                assetReturns = assetReturns,
                months = months(minDate, maxDate),
                request = request
        )

        val timeSeries = ctx.months.fold(listOf<TimeSeries>()) { acc, date ->
            val periodReturn = periodReturn(ctx, date)
            if (acc.isEmpty()) {
                acc + TimeSeries(date = date.toString(), periodReturn = periodReturn, cumulativeReturn = 1.0)
            } else {
                val cumulativeReturn = acc.last().cumulativeReturn
                acc + TimeSeries(
                        date = date.toString(),
                        periodReturn = periodReturn,
                        cumulativeReturn = cumulativeReturn * (1 + periodReturn)
                )
            }
        }

        return SimulatedPerformanceResponse(timeSeries, SummaryMetrics(maximumDrawdown = maximumDrawdown(timeSeries)))
    }

    private fun periodReturn(ctx: Context, date: LocalDate): Double {
        val returns = ctx.request.portfolios.flatMap { portfolio ->
            val portfolioId = portfolio.id
            portfolio.positions.map { position ->
                val positionId = position.id
                val assetId = position.assetId
                val weight = ctx.weights[portfolioId]?.get(positionId) ?: 0.0
                val assetReturn = ctx.assetReturns[date]?.get(assetId)?.value ?: 0.0
                weight * assetReturn
            }
        }
        return returns.sum()
    }

    private fun assetReturns(assetIds: List<String>,
                             start: LocalDate,
                             stop: LocalDate
    ): Map<LocalDate, Map<String, TimeSeriesDatum>> {
        val groupBy = assetTimeSeriesService
                .getMonthlyReturnTimeSeries(assetIds, start, stop)
                .groupBy { LocalDate.parse(it.date) }
        val minDate = groupBy.filter { it.value.size == assetIds.size }.keys.min()
        return groupBy
                .filter { it.key.isAfter(minDate) || it.key.equals(minDate) }
                .mapValues { entry -> entry.value.associateBy { it.assetId } }
    }

    private fun weights(req: SimulatedPerformanceRequest): Map<String, Map<String, Double>> {
        return marketValueAnalysisService
                .marketValueAnalysis(MarketValueAnalysisRequest(req.portfolios))
                .marketValueAnalysis
                .weightsToAllInvestments
    }

}
