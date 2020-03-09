package io.github.erfangc.convexoptimizer

import io.github.erfangc.assets.AssetService
import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.users.UserService
import org.junit.jupiter.api.Test

internal class ConvexOptimizerServiceTest {

    @Test
    fun optimizePortfolio() {
        val assetService = AssetService()
        val marketValueAnalysisService = MarketValueAnalysisService(assetService)
        val assetTimeSeriesService = AssetTimeSeriesService()
        val covarianceService = CovarianceService(assetTimeSeriesService)
        val userService = UserService()

        val svc = ConvexOptimizerService(
                marketValueAnalysisService = marketValueAnalysisService,
                assetService = assetService,
                covarianceService = covarianceService,
                expectedReturnsService = ExpectedReturnsService(assetTimeSeriesService, assetService),
                userService = userService
        )

        val response = svc.optimizePortfolio(OptimizePortfolioRequest(
                objectives = Objectives(0.055),
                newInvestments = 100_000.0
        ))
        response.proposedOrders
    }
}