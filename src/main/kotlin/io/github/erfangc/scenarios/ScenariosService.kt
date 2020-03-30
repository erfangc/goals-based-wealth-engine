package io.github.erfangc.scenarios

import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.assets.models.TimeSeriesDatum
import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.common.DateUtils.months
import io.github.erfangc.common.DateUtils.mostRecentMonthEnd
import io.github.erfangc.common.PortfolioUtils.assetIds
import io.github.erfangc.scenarios.models.*
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Performs regression based scenario analysis
 *
 * Scenario gains and losses at the individual asset level is computed
 * as the shock sizes * the regression beta between the indices being shocked in an scenario analysis
 * and the asset's own return time series
 */
@Service
class ScenariosService(private val assetTimeSeriesService: AssetTimeSeriesService,
                       private val marketValueAnalysisService: MarketValueAnalysisService) {

    /**
     * Scenario analysis is a two step process:
     *
     * 1 - figure out asset gain losses in response to shocks to the defined indices for each scenario (via OLS)
     * 2 - aggregate teh asset level shocks to the portfolio level
     */
    fun scenariosAnalysis(req: ScenariosAnalysisRequest): ScenarioAnalysisResponse {
        val assetGainLosses = assetGainLosses(req)
        return ScenarioAnalysisResponse(computePortfolioScenarioOutputs(req, assetGainLosses))
    }

    private fun computePortfolioScenarioOutputs(req: ScenariosAnalysisRequest,
                                                assetGainLosses: Map<String, Map<ScenarioDefinition, Double>>): List<ScenarioOutput> {
        val (_, _, _, _, weightsToAllInvestments, _) = marketValueAnalysisService
                .marketValueAnalysis(MarketValueAnalysisRequest(req.portfolios))
                .marketValueAnalysis
        return req
                .scenarioDefinitions
                .map { scenarioDefinition ->
                    val portfolioGainLoss = req
                            .portfolios
                            .flatMap { portfolio ->
                                val portfolioId = portfolio.id
                                portfolio.positions.map { position ->
                                    val assetId = position.assetId
                                    val positionId = position.id
                                    val weight = weightsToAllInvestments[portfolioId]?.get(positionId) ?: 0.0
                                    val gainLoss = assetGainLosses[assetId]?.get(scenarioDefinition) ?: 0.0
                                    weight * gainLoss
                                }
                            }
                            .sum()
                    ScenarioOutput(id = scenarioDefinition.id, gainLoss = portfolioGainLoss, scenarioDefinition = scenarioDefinition)
                }
    }

    /**
     * Compute the asset level gains and losses in various scenarios
     */
    private fun assetGainLosses(req: ScenariosAnalysisRequest): Map<String, Map<ScenarioDefinition, Double>> {
        val (months, assetReturnTimeSeries, marketDataTimeSeries) = prepareData(req)
        return assetReturnTimeSeries.keys.map { assetId ->
            val y = months.map { month ->
                assetReturnTimeSeries[assetId]?.get(month)?.value ?: 0.0
            }.toDoubleArray()

            assetId to req.scenarioDefinitions.map { scenarioDefinition ->
                val shocks = scenarioDefinition.scenarioShocks

                // define the x matrix (i.e. a matrix of the factors being shocked)
                // build the "X" from marketDataTimeSeries
                val x = months.map { month ->
                    shocks.map { shock ->
                        marketDataTimeSeries[shock.timeSeriesId]
                                ?.get(month)
                                ?.value ?: 0.0
                    }.toDoubleArray()
                }.toTypedArray()

                // run the regression
                val ols = OLSMultipleLinearRegression()
                ols.newSampleData(y, x)
                val betas = ols.estimateRegressionParameters()
                val shockLookup = shockLookup(scenarioDefinition, shocks.map { it.timeSeriesId })

                // figure out the position of each index to apply the shocks
                // the 1st value is the intercept
                val gainLoss = shockLookup.entries.sumByDouble { (idx, shock) ->
                    betas[idx] * shock.shockSize
                }
                scenarioDefinition to gainLoss
            }.toMap()

        }.toMap()
    }

    /**
     * Prepares and fetches data from the database as well as line up the months etc. so they
     * can be retrieved and looked up easily as maps
     *
     * We return a triple out of convenience, not the best signature even for a private method of course
     */
    private fun prepareData(req: ScenariosAnalysisRequest): Triple<List<LocalDate>, Map<String, Map<LocalDate, TimeSeriesDatum>>, Map<String, Map<LocalDate, TimeSeriesDatum>>> {
        val stop = mostRecentMonthEnd()
        val start = mostRecentMonthEnd().minusYears(5)
        val months = months(start, stop)

        val timeSeriesIds = timeSeriesIds(req)
        val assetIds = assetIds(req.portfolios)

        // dependent variables
        val assetReturnTimeSeries = assetTimeSeriesService
                .getMonthlyReturnTimeSeries(assetIds, start, stop)
                .groupBy { it.assetId }.mapValues { (_, v) -> v.associateBy { LocalDate.parse(it.date) } }

        // independent variables
        val marketDataTimeSeries = assetTimeSeriesService
                .getMonthlyReturnTimeSeries(timeSeriesIds, start, stop)
                .groupBy { it.assetId }.mapValues { (_, v) -> v.associateBy { LocalDate.parse(it.date) } }
        return Triple(months, assetReturnTimeSeries, marketDataTimeSeries)
    }

    private fun shockLookup(scenarioDefinition: ScenarioDefinition, marketIndices: List<String>): Map<Int, ScenarioShock> {
        return scenarioDefinition.scenarioShocks.associateBy { shock ->
            val idx = marketIndices.indexOfFirst { it === shock.timeSeriesId }
            idx + 1
        }
    }

    private fun timeSeriesIds(req: ScenariosAnalysisRequest): List<String> {
        return req.scenarioDefinitions.flatMap { scenarioDefinition ->
            scenarioDefinition.scenarioShocks.map { it.timeSeriesId }
        }.distinct()
    }

}


