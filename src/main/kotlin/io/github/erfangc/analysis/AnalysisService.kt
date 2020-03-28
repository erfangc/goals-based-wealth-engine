package io.github.erfangc.analysis

import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysis
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisResponse
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.scenarios.ScenariosAnalysisRequest
import io.github.erfangc.scenarios.ScenariosService
import io.github.erfangc.users.User
import io.github.erfangc.users.UserService
import io.github.erfangc.util.PortfolioUtils.assetIds
import org.springframework.stereotype.Service
import kotlin.math.sqrt

@Service
class AnalysisService(
        private val marketValueAnalysisService: MarketValueAnalysisService,
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
        private val scenariosService: ScenariosService,
        private val userService: UserService
) {

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

        return AnalysisResponse(
                Analysis(
                        marketValueAnalysis = marketValueAnalysis,
                        expectedReturn = expectedReturn,
                        volatility = volatility,
                        scenarioOutputs = scenarioOutputs
                ),
                assets = marketValueAnalysisResponse.assets
        )
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
                    val portfolioWeight = (marketValueAnalysis.netAssetValues[portfolio.id]
                            ?: 0.0) / marketValueAnalysis.netAssetValue
                    val weights = marketValueAnalysis.weights[portfolio.id]
                    portfolio.positions.map { position ->
                        val assetId = position.assetId
                        val positionId = position.id
                        assetId to (weights?.get(positionId) ?: 0.0) * portfolioWeight
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
            val portfolioWeight = (marketValueAnalysis.netAssetValues[portfolio.id]
                    ?: 0.0) / marketValueAnalysis.netAssetValue
            val weights = marketValueAnalysis.weights
            portfolio.positions.sumByDouble { position ->
                val er = expectedReturns[position.assetId] ?: 0.0
                val wt = (weights[portfolio.id]?.get(position.id) ?: 0.0) * portfolioWeight
                er * wt
            }
        }
    }

    private fun marketValueAnalysis(portfolios: List<Portfolio>): MarketValueAnalysisResponse {
        return marketValueAnalysisService.marketValueAnalysis(MarketValueAnalysisRequest(portfolios))
    }

}