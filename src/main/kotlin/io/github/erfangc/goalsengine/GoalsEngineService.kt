package io.github.erfangc.goalsengine

import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.covariance.ComputeCovariancesResponse
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.users.ModelPortfolioSettings
import io.github.erfangc.users.UserService
import org.springframework.stereotype.Service

@Service
class GoalsEngineService(
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
        private val analysisService: AnalysisService,
        private val userService: UserService
) {

    fun goalsOptimization(req: GoalsOptimizationRequest): GoalsOptimizationResponse {
        val user = userService.getUser()

        val portfolioChoices = if (req.modelPortfolios != null) {
            ModelPortfolioChoices(
                    analysisService = analysisService,
                    modelPortfolioSettings = ModelPortfolioSettings(true, req.modelPortfolios)
            )
        } else {
            // use an efficient frontier
            val whiteList = user.settings?.whiteList ?: emptyList()
            val assetIds = whiteList.map { it.assetId }
            val covariances = covarianceService.computeCovariances(assetIds)
            val expectedReturns = expectedReturns(covariances, assetIds)
            EfficientFrontier(
                    covarianceMatrix = covariances.covariances,
                    expectedReturns = expectedReturns
            )
        }

        // TODO if model portfolios is defined, we should use the model portfolio as the portfolioChoices instead of an efficient frontier
        val goalsEngine = GoalsEngine(
                portfolioChoices = portfolioChoices,
                goal = req.goal,
                initialWealth = req.initialWealth,
                investmentHorizon = req.investmentHorizon,
                cashflows = req.cashflows
        )

        val optimalRiskReward = goalsEngine.findOptimalRiskReward()

        return GoalsOptimizationResponse(
                expectedReturn = optimalRiskReward.expectedReturn,
                volatility = optimalRiskReward.volatility,
                probabilityOfSuccess = optimalRiskReward.probabilityOfSuccess
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

