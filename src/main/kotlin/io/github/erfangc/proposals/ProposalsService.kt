package io.github.erfangc.proposals

import io.github.erfangc.analysis.Analysis
import io.github.erfangc.analysis.AnalysisRequest
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.convexoptimizer.*
import io.github.erfangc.goalsengine.*
import io.github.erfangc.goalsengine.models.*
import io.github.erfangc.goalsengine.portfoliochoices.Cashflow
import io.github.erfangc.goalsengine.portfoliochoices.PortfolioChoices
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.portfolios.PortfolioService
import io.github.erfangc.portfolios.Position
import io.github.erfangc.proposals.models.*
import io.github.erfangc.users.UserService
import io.github.erfangc.users.settings.ModelPortfolio
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDate
import java.util.*
import kotlin.math.pow

private const val supportMsg = "only goals based proposal is supported at the moment"

@Service
class ProposalsService(
        private val userService: UserService,
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
     * The proposal workflow have a few possible steps it might take:
     *
     *  1 - if the client has not been assigned a model portfolio, and that model portfolio assignment is not enabled
     *  the efficient frontier will be used to determine optimal allocat
     */
    fun generateProposal(req: GenerateProposalRequest): GenerateProposalResponse {

        // if the client has an assigned model portfolio, do not run goals optimization, instead just run a probability
        // analysis
        val modelPortfolios = userService
                .currentUser()
                .settings
                .modelPortfolioSettings
                .modelPortfolios

        val (goalsOutput, optimizationResponse) = when (req.client.goals?.approach) {
            "model portfolio" -> {
                if (req.client.goals.autoAssignModelPortfolio == true) {
                    // automatically choose a model portfolio
                    val goalsOutput = modelPortfoliosBasedGoalsOptimization(req, modelPortfolios)
                    (goalsOutput to constrainedTrackingErrorOptimization(goalsOutput, req))
                } else {
                    val modelPortfolio = modelPortfolios.filter { it.id == req.client.modelPortfolioId }
                    // do not run simulation use the probability engine to derive the probability
                    // of reaching the client's goals
                    val goalsOutput = modelPortfoliosBasedGoalsOptimization(req, modelPortfolio)
                    (goalsOutput to constrainedTrackingErrorOptimization(goalsOutput, req))
                }
            }
            else -> {
                // use efficient frontier
                val goalsOutput = efficientFrontierBasedGoalsOptimization(req)
                (goalsOutput to constrainedMeanVarianceOptimization(goalsOutput, req))
            }
        }

        return doPostOptimizationsAnalysis(optimizationResponse, req, goalsOutput)
    }

    private fun originalProbabilityOfSuccess(req: GenerateProposalRequest, analysis: Analysis): Double {
        val riskReward = GoalsEngine(
                portfolioChoices = object : PortfolioChoices {
                    override fun mus(): List<Double> {
                        return listOf(analysis.expectedReturn)
                    }

                    override fun sigma(mu: Double): Double {
                        return analysis.volatility
                    }

                    override fun muMax(): Double {
                        return analysis.expectedReturn
                    }

                    override fun muMin(): Double {
                        return analysis.expectedReturn
                    }
                },
                cashflows = cashflows(req),
                initialWealth = initialInvestment(req),
                goal = goal(req),
                investmentHorizon = investmentHorizon(req)
        ).findOptimalRiskReward()
        return riskReward.probabilityOfSuccess
    }

    /**
     * Perform any post goals / convex optimization step analysis (this is the simple stuff like computing the
     * final portfolio's risk & returns)
     */
    private fun doPostOptimizationsAnalysis(
            optimizationResponse: ConvexOptimizationResponse,
            req: GenerateProposalRequest,
            goalsOutput: GoalsOptimizationOutput
    ): GenerateProposalResponse {

        val proposedAnalysisResponse = analysisService.analyze(AnalysisRequest(optimizationResponse.proposedPortfolios))
        val originalAnalysisResponse = analysisService.analyze(AnalysisRequest(optimizationResponse.originalPortfolios))

        val proposal = Proposal(
                id = UUID.randomUUID().toString(),
                portfolios = portfolioDefinitions(req).map { it.portfolio },
                proposedOrders = optimizationResponse.proposedOrders
        )

        if (req.save) {
            proposalCrudService.saveProposal(proposal)
        }

        val oAnalysis = originalAnalysisResponse.analysis
        val pAnalysis = proposedAnalysisResponse.analysis

        val originalProbabilityOfSuccess = originalProbabilityOfSuccess(req, oAnalysis)
        return GenerateProposalResponse(
                proposal = proposal,
                proposedPortfolios = optimizationResponse.proposedPortfolios,
                analyses = Analyses(
                        expectedReturns = ExpectedReturns(
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
                                original = originalProbabilityOfSuccess,
                                proposed = goalsOutput.probabilityOfSuccess
                        ),
                        allocations = GenerateProposalResponseAllocations(
                                original = oAnalysis.marketValueAnalysis.allocations,
                                proposed = pAnalysis.marketValueAnalysis.allocations
                        ),
                        netAssetValue = NetAssetValue(
                                original = oAnalysis.marketValueAnalysis.netAssetValue,
                                proposed = oAnalysis.marketValueAnalysis.netAssetValue
                        ),
                        scenarioOutputs = ScenarioOutputs(
                                original = oAnalysis.scenarioOutputs,
                                proposed = pAnalysis.scenarioOutputs
                        ),
                        simulatedPerformances = SimulatedPerformances(
                                original = SimulatedPerformance(oAnalysis.simulatedPerformance, oAnalysis.simulatedPerformanceSummaryMetrics),
                                proposed = SimulatedPerformance(pAnalysis.simulatedPerformance, pAnalysis.simulatedPerformanceSummaryMetrics)
                        )
                ),
                assets = proposedAnalysisResponse.assets
        )
    }

    private fun modelPortfoliosBasedGoalsOptimization(req: GenerateProposalRequest,
                                                      modelPortfolios: List<ModelPortfolio>): ModelPortfolioBasedGoalsOptimizationResponse {
        val stopWatch = StopWatch()
        stopWatch.start()
        log.info("Running goals engine to figure out the best model portfolio for ${req.client.id}")
        val goalsOutput = goalsEngineService.modelPortfolioBasedGoalsOptimization(
                ModelPortfolioBasedGoalsOptimizationRequest(
                        initialWealth = initialInvestment(req),
                        cashflows = cashflows(req),
                        investmentHorizon = investmentHorizon(req),
                        goal = goal(req),
                        modelPortfolios = modelPortfolios
                )
        )
        stopWatch.stop()
        log.info("Finished goals engine to figure out the best model portfolio for ${req.client.id}," +
                " probabilityOfSuccess=${goalsOutput.probabilityOfSuccess}, " +
                " expectedReturn=${goalsOutput.expectedReturn}, " +
                " volatility=${goalsOutput.volatility}, " +
                " modelPortfolio.id=${goalsOutput.modelPortfolio.id}, " +
                " modelPortfolio.name=${goalsOutput.modelPortfolio.portfolio.name}, " +
                " run time: ${stopWatch.lastTaskTimeMillis} ms")

        return goalsOutput
    }

    private fun efficientFrontierBasedGoalsOptimization(req: GenerateProposalRequest): EfficientFrontierBasedGoalsOptimizationResponse {
        val stopWatch = StopWatch()
        log.info("Running goals engine to figure out the best risk reward for ${req.client.id}")
        val whiteListItems = userService.currentUser().settings.whiteList
        stopWatch.start()
        val goalsOutput = goalsEngineService.efficientFrontierBasedGoalsOptimization(
                EfficientFrontierBasedGoalsOptimizationRequest(
                        initialWealth = initialInvestment(req),
                        cashflows = cashflows(req),
                        investmentHorizon = investmentHorizon(req),
                        goal = goal(req),
                        whiteListItems = whiteListItems
                )
        )
        stopWatch.stop()
        log.info("Finished goals engine to figure out the best risk reward for ${req.client.id}," +
                " probabilityOfSuccess=${goalsOutput.probabilityOfSuccess}, " +
                " expectedReturn=${goalsOutput.expectedReturn}, " +
                " volatility=${goalsOutput.volatility}, " +
                " run time: ${stopWatch.lastTaskTimeMillis} ms")

        return goalsOutput
    }

    private fun constrainedTrackingErrorOptimization(goalsOutput: ModelPortfolioBasedGoalsOptimizationResponse,
                                                     req: GenerateProposalRequest): ConvexOptimizationResponse {
        val modelPortfolio = goalsOutput.modelPortfolio
        return convexOptimizerService.constrainedTrackingErrorOptimization(
                ConstrainedTrackingErrorOptimizationRequest(
                        portfolios = portfolioDefinitions(req, modelPortfolio),
                        modelPortfolio = modelPortfolio
                )
        )
    }

    private fun constrainedMeanVarianceOptimization(
            efficientFrontierBasedGoalsOutput: EfficientFrontierBasedGoalsOptimizationResponse,
            req: GenerateProposalRequest
    ): ConvexOptimizationResponse {
        val stopWatch = StopWatch()
        val expectedReturn = efficientFrontierBasedGoalsOutput.expectedReturn
        log.info("Running convex optimization to target expected return ${expectedReturn * 100}% ${req.client.id}")
        stopWatch.start()
        val optimizePortfolioResponse = convexOptimizerService.constrainedMeanVarianceOptimization(
                ConstrainedMeanVarianceOptimizationRequest(
                        objectives = Objectives(expectedReturn = expectedReturn),
                        portfolios = portfolioDefinitions(req)
                )
        )
        stopWatch.stop()
        log.info("Finished convex optimization to target expected return" +
                " ${expectedReturn * 100}% ${req.client.id}, run time: ${stopWatch.lastTaskTimeMillis} ms")
        return optimizePortfolioResponse
    }

    /**
     * Create the portfolio definitions for convex optimization. This steps
     * creates any new portfolios as necessary (if new funds are used) and resolves the white list
     * for each portfolio
     */
    private fun portfolioDefinitions(req: GenerateProposalRequest, modelPortfolio: ModelPortfolio? = null): List<PortfolioDefinition> {

        // use the whitelist resolver to populate white list per portfolio
        val whiteListResolver = WhiteListResolver(userService)

        val existingDefinitions = portfolioService
                .getForClientId(req.client.id)
                ?.map { portfolio ->
                    val withdrawRestricted = listOf("ira", "401k").contains(portfolio.source?.subType)
                    val whiteListItems = whiteListResolver.resolveWhiteListItems(ResolveWhiteListItemRequest(portfolio, modelPortfolio)).whiteListItems
                    PortfolioDefinition(
                            portfolio = portfolio,
                            withdrawRestricted = withdrawRestricted,
                            whiteList = whiteListItems
                    )
                }

        val newPortfolio = listOfNotNull(
                if (req.newInvestment != null && req.newInvestment > 0) {
                    val portfolio = Portfolio(
                            id = "new-portfolio",
                            name = "New Portfolio",
                            // position line item representing the new investment
                            positions = listOf(Position(id = "CASH", assetId = "USD", quantity = req.newInvestment))
                    )
                    val whiteList = whiteListResolver.resolveWhiteListItems(ResolveWhiteListItemRequest(portfolio, modelPortfolio)).whiteListItems
                    PortfolioDefinition(
                            portfolio = portfolio,
                            whiteList = whiteList
                    )
                } else {
                    null
                }
        )
        val ret = ((existingDefinitions ?: emptyList()) + newPortfolio)
                // get rid of any portfolios that might not have a position
                .filter { it.portfolio.positions.isNotEmpty() }
        if (ret.isEmpty()) {
            throw IllegalStateException("Unable to initiate optimization since the client has no existing holdings nor putting in new investments")
        } else {
            return ret
        }
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
        return (existingInvestments ?: 0.0) + (req.newInvestment ?: 0.0)
    }

}
