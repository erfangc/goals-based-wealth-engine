package io.github.erfangc.proposals

import io.github.erfangc.analysis.AnalysisRequest
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.convexoptimizer.ConvexOptimizerService
import io.github.erfangc.convexoptimizer.Objectives
import io.github.erfangc.convexoptimizer.OptimizePortfolioRequest
import io.github.erfangc.convexoptimizer.PortfolioDefinition
import io.github.erfangc.goalsengine.Cashflow
import io.github.erfangc.goalsengine.GoalsEngineService
import io.github.erfangc.goalsengine.GoalsOptimizationRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.portfolios.PortfolioService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDate
import java.util.*
import kotlin.math.pow

private const val supportMsg = "only goals based proposal is supported at the moment"

@Service
class ProposalsService(
        private val analysisService: AnalysisService,
        private val proposalCrudService: ProposalCrudService,
        private val goalsEngineService: GoalsEngineService,
        private val portfolioService: PortfolioService,
        private val marketValueAnalysisService: MarketValueAnalysisService,
        private val convexOptimizerService: ConvexOptimizerService
) {

    private val log = LoggerFactory.getLogger(ProposalsService::class.java)

    /**
     * Initiate the workflow of creating a proposal
     *
     * In goals based proposal workflow, we first seek an optimal portfolio along the efficient frontier
     * and then use convex optimization to target that portfolio given all constraints
     */
    fun generateProposal(req: GenerateProposalRequest): GenerateProposalResponse {
        // step 1 - run goals based simulation to find risk reward that maximizes chance of success
        log.info("Running goals engine to figure out the best risk reward for ${req.client.id}")
        val stopWatch = StopWatch()
        stopWatch.start()
        val investmentHorizon = investmentHorizon(req)
        val cashflows = cashflows(req)
        val initialWealth = initialInvestment(req)
        val goal = goal(req)
        val goalsOutput = goalsEngineService.goalsOptimization(
                GoalsOptimizationRequest(
                        initialWealth = initialWealth,
                        cashflows = cashflows,
                        investmentHorizon = investmentHorizon,
                        goal = goal
                )
        )
        stopWatch.stop()
        log.info("Finished goals engine to figure out the best risk reward for ${req.client.id}," +
                " probabilityOfSuccess=${goalsOutput.probabilityOfSuccess}, " +
                " volatility=${goalsOutput.volatility}, " +
                " run time: ${stopWatch.lastTaskTimeMillis} ms")

        val expectedReturn = goalsOutput.expectedReturn
        // step 2 - given the target risk reward, use convex optimization to find the actual trades
        log.info("Running convex optimization to target expected return ${expectedReturn * 100}% ${req.client.id}")
        stopWatch.start()
        val optimizePortfolioResponse = convexOptimizerService.optimizePortfolio(
                OptimizePortfolioRequest(
                        newInvestments = req.newInvestment,
                        objectives = Objectives(expectedReturn = expectedReturn),
                        portfolios = portfolioDefinitions(req)
                )
        )
        stopWatch.stop()
        log.info("Finished convex optimization to target expected return ${expectedReturn * 100}% ${req.client.id}, run time: ${stopWatch.lastTaskTimeMillis} ms")

        val portfolios = portfolioDefinitions(req)?.map { it.portfolio } ?: emptyList()
        val proposedAnalysisResponse = analysisService.analyze(AnalysisRequest(optimizePortfolioResponse.proposedPortfolios))
        val originalAnalysisResponse = analysisService.analyze(AnalysisRequest(optimizePortfolioResponse.originalPortfolios))

        val proposal = Proposal(
                id = UUID.randomUUID().toString(),
                portfolios = portfolios,
                proposedOrders = optimizePortfolioResponse.proposedOrders
        )

        if (req.save) {
            proposalCrudService.saveProposal(proposal)
        }
        val oAnalysis = originalAnalysisResponse.analysis
        val pAnalysis = proposedAnalysisResponse.analysis
        return GenerateProposalResponse(
                proposal = proposal,
                proposedPortfolios = optimizePortfolioResponse.proposedPortfolios,
                analyses = Analyses(
                        expectedReturn = ExpectedReturn(
                                original = oAnalysis.expectedReturn,
                                proposed = pAnalysis.expectedReturn
                        ),
                        weights = Weights(
                                original = oAnalysis.marketValueAnalysis.weights,
                                proposed = pAnalysis.marketValueAnalysis.weights
                        ),
                        marketValue = MarketValue(
                                original = oAnalysis.marketValueAnalysis.marketValue,
                                proposed = pAnalysis.marketValueAnalysis.marketValue
                        ),
                        volatility = Volatility(
                                original = oAnalysis.volatility,
                                proposed = pAnalysis.volatility
                        ),
                        probabilityOfSuccess = ProbabilityOfSuccess(
                                original = 0.0, // TODO
                                proposed = goalsOutput.probabilityOfSuccess
                        ),
                        allocations = GenerateProposalResponseAllocations(
                                original = oAnalysis.marketValueAnalysis.allocations,
                                proposed = pAnalysis.marketValueAnalysis.allocations
                        ),
                        netAssetValue = NetAssetValue(
                                original = oAnalysis.marketValueAnalysis.netAssetValue,
                                proposed = oAnalysis.marketValueAnalysis.netAssetValue
                        )
                ),
                assets = proposedAnalysisResponse.assets
        )
    }

    /**
     * Grab existing portfolios
     */
    private fun portfolioDefinitions(req: GenerateProposalRequest) =
            portfolioService
                    .getForClientId(req.client.id)
                    ?.map {
                        portfolio ->
                        val withdrawRestricted = listOf("ira", "401k").contains(portfolio.source?.subType)
                        PortfolioDefinition(portfolio = portfolio, withdrawRestricted = withdrawRestricted)
                    }

    /**
     * Derive the goal (lump sum) amount to target at retirement
     */
    private fun goal(req: GenerateProposalRequest): Double {
        val goals = req.client.goals ?: error(supportMsg)
        val requiredIncome = goals.retirementYearlyIncome - goals.supplementalYearlyIncome
        // TODO we need to come up with assumptions and calculations for decumulation so we can compute the lump sum
        val n = 30
        val r = 0.03
        return requiredIncome * ((1 - (1 / (1 + r).pow(n))) / r)
    }

    /**
     * Derive investment horizon based on client goals
     */
    private fun investmentHorizon(req: GenerateProposalRequest): Int {
        val retirementYear = req.client.goals?.retirement ?: error(supportMsg)
        val year = LocalDate.now().year
        return retirementYear.year - year
    }

    /**
     * Simple conversion of known cashflows from date vs. year until format
     */
    private fun cashflows(req: GenerateProposalRequest): List<Cashflow> {
        val year = LocalDate.now().year
        return req
                .client
                .goals
                ?.knownCashflows
                ?.map { Cashflow(t = it.year - year, amount = it.amount) }
                ?: error(supportMsg)
    }

    /**
     * Find the initial investment, which is the sum of all portfolio today + any new money the client is looking to
     * invest
     */
    private fun initialInvestment(req: GenerateProposalRequest): Double {
        val clientId = req.client.id
        val existingInvestments = portfolioService.getForClientId(clientId)?.let { portfolios ->
            marketValueAnalysisService.marketValueAnalysis(MarketValueAnalysisRequest(portfolios))
                    .marketValueAnalysis
                    .netAssetValue
        }
        return (existingInvestments ?: 0.0) + req.newInvestment
    }

}
