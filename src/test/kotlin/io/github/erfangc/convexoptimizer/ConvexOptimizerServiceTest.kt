package io.github.erfangc.convexoptimizer

import io.github.erfangc.assets.AssetService
import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.users.UserService
import io.github.erfangc.util.WeightComputer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ConvexOptimizerServiceTest {

    @Test
    fun optimizePortfolio() {
        val assetService = AssetService()
        val weightComputer = WeightComputer(assetService)
        val assetTimeSeriesService = AssetTimeSeriesService()
        val covarianceService = CovarianceService(assetTimeSeriesService)
        val userService = UserService()

        val svc = ConvexOptimizerService(
                weightComputer = weightComputer,
                assetService = assetService,
                covarianceService = covarianceService,
                expectedReturnsService = ExpectedReturnsService(assetTimeSeriesService, assetService),
                userService = userService
        )

        val response = svc.optimizePortfolio(OptimizePortfolioRequest(objectives = Objectives(0.055)))
        response.proposedOrders

    }
}