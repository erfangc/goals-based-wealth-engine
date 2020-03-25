package io.github.erfangc.convexoptimizer

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.assets.AssetService
import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.users.UserService
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class ConvexOptimizerServiceTest {

    @Test
    fun optimizePortfolio() {
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val assetService = AssetService(ddb)
        val assetTimeSeriesService = AssetTimeSeriesService(ddb)
        val marketValueAnalysisService = MarketValueAnalysisService(assetService)
        val expectedReturnsService = ExpectedReturnsService(assetTimeSeriesService, assetService)
        val covarianceService = CovarianceService(assetTimeSeriesService)
        val analysisService = AnalysisService(marketValueAnalysisService, expectedReturnsService, covarianceService)
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val userService = UserService(jdbcTemplate = mockk(), objectMapper = objectMapper)

        val svc = ConvexOptimizerService(
                analysisService = analysisService,
                assetService = assetService,
                covarianceService = covarianceService,
                expectedReturnsService = expectedReturnsService,
                userService = userService
        )

        val response = svc.optimizePortfolio(OptimizePortfolioRequest(
                objectives = Objectives(0.055),
                newInvestments = 100_000.0
        ))
        response.proposedOrders
    }
}