package io.github.erfangc.proposals

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
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
import io.github.erfangc.proposals.models.GenerateProposalRequest
import io.github.erfangc.scenarios.ScenariosService
import io.github.erfangc.users.UserService
import io.github.erfangc.util.DynamoDBUtil.objectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate

import java.util.*

internal class ProposalsServiceTest {

    @Test
    fun generateProposal() {

        /*
        wiring dependencies
         */
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val assetService = AssetService(ddb)
        val assetTimeSeriesService = AssetTimeSeriesService(ddb)
        val userService = UserService(jdbcTemplate = mockk(), objectMapper = objectMapper)
        val covarianceService = CovarianceService(assetTimeSeriesService)
        val expectedReturnsService = ExpectedReturnsService(assetTimeSeriesService = assetTimeSeriesService, assetService = assetService)
        val marketValueAnalysisService = MarketValueAnalysisService(assetService = assetService)
        val scenariosService = ScenariosService(assetTimeSeriesService, marketValueAnalysisService)
        val analysisService = AnalysisService(marketValueAnalysisService, expectedReturnsService, covarianceService, scenariosService, userService)

        val goalsEngineService = GoalsEngineService(
                analysisService = analysisService,
                expectedReturnsService = expectedReturnsService,
                covarianceService = covarianceService
        )
        val convexOptimizerService = ConvexOptimizerService(
                analysisService = analysisService,
                assetService = assetService,
                covarianceService = covarianceService,
                expectedReturnsService = expectedReturnsService
        )
        val jdbcTemplate = mockk<NamedParameterJdbcTemplate>()
        every {
            jdbcTemplate.queryForList(any(), any<Map<String, *>>())
        } returns emptyList()
        val portfolioService = PortfolioService(userService, jdbcTemplate)

        val proposalCrudService = ProposalCrudService(userService, jacksonObjectMapper(), jdbcTemplate)

        val svc = ProposalsService(
                goalsEngineService = goalsEngineService,
                marketValueAnalysisService = marketValueAnalysisService,
                convexOptimizerService = convexOptimizerService,
                portfolioService = portfolioService,
                analysisService = analysisService,
                proposalCrudService = proposalCrudService,
                userService = userService
        )

        val response = svc.generateProposal(
                req = GenerateProposalRequest(
                        save = false,
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