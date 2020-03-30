package io.github.erfangc.analysis

import io.github.erfangc.analysis.models.Analysis
import io.github.erfangc.analysis.models.AnalysisRequest
import io.github.erfangc.analysis.models.AnalysisResponse
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.goalsengine.ClientGoalsTranslatorService
import io.github.erfangc.goalsengine.internal.GoalsEngine
import io.github.erfangc.goalsengine.models.TranslateClientGoalsRequest
import io.github.erfangc.goalsengine.models.PortfolioChoices
import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysis
import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysisResponse
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.scenarios.models.ScenariosAnalysisRequest
import io.github.erfangc.scenarios.ScenariosService
import io.github.erfangc.simulatedperformance.SimulatedPerformanceService
import io.github.erfangc.simulatedperformance.models.MaximumDrawdown
import io.github.erfangc.simulatedperformance.models.SimulatedPerformanceRequest
import io.github.erfangc.simulatedperformance.models.SimulatedPerformanceResponse
import io.github.erfangc.simulatedperformance.models.SummaryMetrics
import io.github.erfangc.users.models.User
import io.github.erfangc.users.UserService
import io.github.erfangc.common.PortfolioUtils.assetIds
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.sqrt

@Service
class AnalysisService(
        private val marketValueAnalysisService: MarketValueAnalysisService,
        private val simulatedPerformanceService: SimulatedPerformanceService,
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
        private val scenariosService: ScenariosService,
        private val userService: UserService,
        private val clientGoalsTranslatorService: ClientGoalsTranslatorService
) {

    private val log = LoggerFactory.getLogger(AnalysisService::class.java)

    private fun probabilityOfSuccess(req: AnalysisRequest, analysis: Analysis): Double? {
        val client = req.client ?: return null
        val (goal, initialInvestment, cashflows, investmentHorizon) = clientGoalsTranslatorService
                .translate(TranslateClientGoalsRequest(client, req.portfolios))
        val riskReward = GoalsEngine(
                portfolioChoices = object : PortfolioChoices {
                    override fun mus(): List<Double> {
                        return listOf(analysis.expectedReturn)
                    }

                    override fun sigma(mu: Double): Double {
                        return analysis.volatility
                    }

                    override fun muMax(): Double {
                        return analysis.expectedReturn
                    }

                    override fun muMin(): Double {
                        return analysis.expectedReturn
                    }
                },
                cashflows = cashflows,
                initialWealth = initialInvestment,
                goal = goal,
                investmentHorizon = investmentHorizon
        ).findOptimalRiskReward()
        return riskReward.probabilityOfSuccess
    }

    fun analyze(req: AnalysisRequest): AnalysisResponse {
        val user = userService.currentUser()

        // flatten the group of portfolios into a single one
        val portfolios = req.portfolios
        val marketValueAnalysisResponse = marketValueAnalysis(portfolios)

        // compute variance
        val marketValueAnalysis = marketValueAnalysisResponse.marketValueAnalysis
        val expectedReturn = expectedReturn(portfolios, marketValueAnalysis)
        val volatility = volatility(portfolios, marketValueAnalysis)
        val scenarioOutputs = scenarioOutputs(req, user)
        val simulatedPerformance = simulatedPerformance(req)

        val analysis = Analysis(
                marketValueAnalysis = marketValueAnalysis,
                expectedReturn = expectedReturn,
                volatility = volatility,
                scenarioOutputs = scenarioOutputs,
                simulatedPerformance = simulatedPerformance.timeSeries,
                simulatedPerformanceSummaryMetrics = simulatedPerformance.summaryMetrics
        )

        val probabilityOfSuccess = probabilityOfSuccess(req = req, analysis = analysis)

        return AnalysisResponse(
                analysis = analysis.copy(probabilityOfSuccess = probabilityOfSuccess),
                assets = marketValueAnalysisResponse.assets
        )
    }

    private fun simulatedPerformance(req: AnalysisRequest): SimulatedPerformanceResponse {
        return try {
            simulatedPerformanceService.analyze(SimulatedPerformanceRequest(req.portfolios))
        } catch (e: Exception) {
            log.error("Unable to compute simulated performance", e)
            SimulatedPerformanceResponse(timeSeries = emptyList(), summaryMetrics = SummaryMetrics(MaximumDrawdown()))
        }
    }


    private fun scenarioOutputs(req: AnalysisRequest, user: User) =
            scenariosService.scenariosAnalysis(ScenariosAnalysisRequest(req.portfolios, user.settings.scenarioDefinitions)).scenarioOutputs

    private fun volatility(portfolios: List<Portfolio>, marketValueAnalysis: MarketValueAnalysis): Double {
        if (portfolios.isEmpty()) {
            return 0.0
        }
        val (covariances, assetIndexLookup) = covarianceService.computeCovariances(assetIds(portfolios))
        val assetWeights = portfolios
                .flatMap { portfolio ->
                    val weights = marketValueAnalysis.weightsToAllInvestments[portfolio.id]
                    portfolio.positions.map { position ->
                        val assetId = position.assetId
                        val positionId = position.id
                        assetId to (weights?.get(positionId) ?: 0.0)
                    }
                }
                .groupBy { it.first }
                .mapValues { (_, weights) ->
                    weights.sumByDouble { it.second }
                }

        val variance = assetWeights.flatMap { (assetId1, w1) ->
            val a1 = assetIndexLookup[assetId1] ?: error("")
            assetWeights.map { (assetId2, w2) ->
                val a2 = assetIndexLookup[assetId2] ?: error("")
                w1 * w2 * covariances[a1][a2]
            }
        }.sum()

        return sqrt(variance)
    }

    private fun expectedReturn(portfolios: List<Portfolio>,
                               marketValueAnalysis: MarketValueAnalysis): Double {
        // compute expected returns
        val expectedReturns = expectedReturnsService.getExpectedReturns(assetIds(portfolios))
        return portfolios.sumByDouble { portfolio ->
            val weights = marketValueAnalysis.weightsToAllInvestments
            portfolio.positions.sumByDouble { position ->
                val er = expectedReturns[position.assetId]?.expectedReturn ?: 0.0
                val wt = (weights[portfolio.id]?.get(position.id) ?: 0.0)
                er * wt
            }
        }
    }

    private fun marketValueAnalysis(portfolios: List<Portfolio>): MarketValueAnalysisResponse {
        return marketValueAnalysisService.marketValueAnalysis(MarketValueAnalysisRequest(portfolios))
    }

}