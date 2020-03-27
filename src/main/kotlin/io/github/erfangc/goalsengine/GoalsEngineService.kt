package io.github.erfangc.goalsengine

import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.covariance.ComputeCovariancesResponse
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import org.springframework.stereotype.Service

@Service
class GoalsEngineService(
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
        private val analysisService: AnalysisService
) {
    /**
     * This goals based analysis uses an implementation of [PortfolioChoices] that
     * selects portfolios based a predetermined discrete portfolio set (i.e. a group of model portfolios)
     * instead of using an efficient frontier
     */
    fun modelPortfolioBasedGoalsOptimization(req: ModelPortfolioBasedGoalsOptimizationRequest): ModelPortfolioBasedGoalsOptimizationResponse {
        val modelPortfolioChoices = ModelPortfolioChoices(analysisService, req.modelPortfolios)
        val goalsEngine = GoalsEngine(
                portfolioChoices = modelPortfolioChoices,
                goal = req.goal,
                initialWealth = req.initialWealth,
                investmentHorizon = req.investmentHorizon,
                cashflows = req.cashflows
        )

        val optimalRiskReward = goalsEngine.findOptimalRiskReward()

        return ModelPortfolioBasedGoalsOptimizationResponse(
                expectedReturn = optimalRiskReward.expectedReturn,
                volatility = optimalRiskReward.volatility,
                probabilityOfSuccess = optimalRiskReward.probabilityOfSuccess,
                modelPortfolio = modelPortfolioChoices.getPortfolio(optimalRiskReward.expectedReturn)
        )
    }

    /**
     * This goals based optimization finds the optimal outcome by picking points from a continuous and dynamically generated
     * efficient frontier
     */
    fun efficientFrontierBasedGoalsOptimization(req: EfficientFrontierBasedGoalsOptimizationRequest): EfficientFrontierBasedGoalsOptimizationResponse {
        val portfolioChoices = efficientFrontier(req)
        val goalsEngine = GoalsEngine(
                portfolioChoices = portfolioChoices,
                goal = req.goal,
                initialWealth = req.initialWealth,
                investmentHorizon = req.investmentHorizon,
                cashflows = req.cashflows
        )

        val optimalRiskReward = goalsEngine.findOptimalRiskReward()

        return EfficientFrontierBasedGoalsOptimizationResponse(
                expectedReturn = optimalRiskReward.expectedReturn,
                volatility = optimalRiskReward.volatility,
                probabilityOfSuccess = optimalRiskReward.probabilityOfSuccess
        )
    }

    private fun efficientFrontier(req: EfficientFrontierBasedGoalsOptimizationRequest): EfficientFrontier {
        // use an efficient frontier
        val whiteList = req.whiteListItems
        val assetIds = whiteList.map { it.assetId }
        val covariances = covarianceService.computeCovariances(assetIds)
        val expectedReturns = expectedReturns(covariances, assetIds)
        return EfficientFrontier(
                covarianceMatrix = covariances.covariances,
                expectedReturns = expectedReturns
        )
    }

    private fun expectedReturns(covariances: ComputeCovariancesResponse, assetIds: List<String>): DoubleArray {
        val lookup = covariances.assetIndexLookup.map { it.value to it.key }.toMap()
        val expectedReturns = expectedReturnsService.getExpectedReturns(assetIds)
        return covariances.covariances.indices.map { idx ->
            expectedReturns[lookup[idx]] ?: 0.0
        }.toDoubleArray()
    }

}

