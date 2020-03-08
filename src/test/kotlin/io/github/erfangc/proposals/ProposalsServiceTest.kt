package io.github.erfangc.proposals

import io.github.erfangc.assets.AssetService
import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.clients.Client
import io.github.erfangc.clients.Goals
import io.github.erfangc.convexoptimizer.ConvexOptimizerService
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.goalsengine.GoalsEngineService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.portfolios.PortfolioService
import io.github.erfangc.users.UserService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.util.*

internal class ProposalsServiceTest {

    @Test
    fun generateProposal() {

        /*
        wiring dependencies
         */
        val assetService = AssetService()
        val assetTimeSeriesService = AssetTimeSeriesService()
        val userService = UserService()
        val covarianceService = CovarianceService(assetTimeSeriesService)
        val expectedReturnsService = ExpectedReturnsService(assetTimeSeriesService = assetTimeSeriesService, assetService = assetService)
        val goalsEngineService = GoalsEngineService(
                expectedReturnsService = expectedReturnsService,
                userService = userService,
                covarianceService = covarianceService
        )
        val marketValueAnalysisService = MarketValueAnalysisService(assetService)
        val convexOptimizerService = ConvexOptimizerService(
                marketValueAnalysisService = marketValueAnalysisService,
                assetService = assetService,
                covarianceService = covarianceService,
                userService = userService,
                expectedReturnsService = expectedReturnsService
        )
        val portfolioService = PortfolioService()

        val svc = ProposalsService(
                goalsEngineService = goalsEngineService,
                marketValueAnalysisService = marketValueAnalysisService,
                convexOptimizerService = convexOptimizerService,
                portfolioService = portfolioService
        )

        val response = svc.generateProposal(
                req = GenerateProposalRequest(
                        client = Client(
                                id = UUID.randomUUID().toString(),
                                lastName = "Erfang",
                                firstName = "Chen",
                                goals = Goals(
                                        retirementYear = 2030,
                                        retirementYearlyIncome = 2000.0
                                )
                        ),
                        newInvestment = 100_000.0
                )
        )
        response.proposal
    }
}