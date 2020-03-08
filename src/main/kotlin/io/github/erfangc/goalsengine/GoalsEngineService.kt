package io.github.erfangc.goalsengine

import io.github.erfangc.covariance.ComputeCovariancesResponse
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.users.UserService
import org.springframework.stereotype.Service

@Service
class GoalsEngineService(
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
        private val userService: UserService
) {

    fun goalsOptimization(req: GoalsOptimizationRequest): GoalsOptimizationResponse {
        val user = userService.getUser()
        // use an efficient frontier
        val whiteList = user.overrides?.whiteList ?: emptyList()
        val assetIds = whiteList.map { it.assetId }
        val covariances = covarianceService.computeCovariances(assetIds)
        val expectedReturns = expectedReturns(covariances, assetIds)
        val goalsEngine = GoalsEngine(
                portfolioChoices = EfficientFrontier(
                        covarianceMatrix = covariances.covariances,
                        expectedReturns = expectedReturns
                ),
                goal = req.goal,
                initialWealth = req.initialWealth,
                investmentHorizon = req.investmentHorizon,
                knownCashflows = req.cashflows
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

