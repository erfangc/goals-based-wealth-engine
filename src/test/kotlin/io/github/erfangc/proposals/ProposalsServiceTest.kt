package io.github.erfangc.proposals

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.erfangc.analysis.AnalysisService
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
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
        val portfolioService = PortfolioService(userService, mockk())

        val analysisService = AnalysisService(marketValueAnalysisService, expectedReturnsService, covarianceService)
        val proposalCrudService = ProposalCrudService(userService, jacksonObjectMapper(), mockk())

        val svc = ProposalsService(
                goalsEngineService = goalsEngineService,
                marketValueAnalysisService = marketValueAnalysisService,
                convexOptimizerService = convexOptimizerService,
                portfolioService = portfolioService,
                analysisService = analysisService,
                proposalCrudService = proposalCrudService
        )

        val response = svc.generateProposal(
                req = GenerateProposalRequest(
                        client = Client(
                                id = UUID.randomUUID().toString(),
                                lastName = "Erfang",
                                firstName = "Chen",
                                goals = Goals(
                                        retirement = LocalDate.of(2035, 1, 1),
                                        retirementYearlyIncome = 10000.0 * 12
                                )
                        ),
                        newInvestment = 800_000.0
                )
        )
        println(response.proposal)
    }
}