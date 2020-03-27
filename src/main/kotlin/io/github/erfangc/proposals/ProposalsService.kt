package io.github.erfangc.proposals

import io.github.erfangc.analysis.AnalysisRequest
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.convexoptimizer.*
import io.github.erfangc.goalsengine.*
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
     * In goals based proposal workflow, we first seek an optimal portfolio along the efficient frontier
     * and then use convex optimization to target that portfolio given all constraints
     */
    fun generateProposal(req: GenerateProposalRequest): GenerateProposalResponse {

        // if the client has an assigned model portfolio, do not run goals optimization, instead just run a probability
        // analysis
        val (goalsOutput, optimizationResponse) = when {
            req.client.goals?.approach == "model portfolio" -> {
                if (req.client.goals.autoAssignModelPortfolio == true) {
                    // automatically choose a model portfolio
                    val goalsOutput = modelPortfoliosBasedGoalsOptimization(req)
                    (goalsOutput to constrainedTrackingErrorOptimization(goalsOutput, req))
                } else {
                    // do not run simulation use the probability engine to derive the probability
                    // of reaching the client's goals
                    TODO("run a simple analysis without going through the goals engine")
                }
            } else -> {
                // use efficient frontier
                val goalsOutput = efficientFrontierBasedGoalsOptimization(req)
                (goalsOutput to constrainedMeanVarianceOptimization(goalsOutput, req))
            }
        }

        return doPostOptimizationsAnalysis(optimizationResponse, req, goalsOutput)
    }

    /**
     * Perform any post goals / convex optimization step analysis (this is the simple stuff like computing the
     * final portfolio's risk & returns)
     */
    private fun doPostOptimizationsAnalysis(optimizationResponse: ConvexOptimizationResponse,
                                            req: GenerateProposalRequest,
                                            goalsOutput: GoalsOptimizationOutput): GenerateProposalResponse {
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

        return GenerateProposalResponse(
                proposal = proposal,
                proposedPortfolios = optimizationResponse.proposedPortfolios,
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
                                original = 0.0, // TODO use the ProbabilityEngine to fill this in
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

    private fun modelPortfoliosBasedGoalsOptimization(req: GenerateProposalRequest): ModelPortfolioBasedGoalsOptimizationResponse {
        val stopWatch = StopWatch()
        stopWatch.start()
        log.info("Running goals engine to figure out the best model portfolio for ${req.client.id}")
        val goalsOutput = goalsEngineService.modelPortfolioBasedGoalsOptimization(
                ModelPortfolioBasedGoalsOptimizationRequest(
                        initialWealth = initialInvestment(req),
                        cashflows = cashflows(req),
                        investmentHorizon = investmentHorizon(req),
                        goal = goal(req),
                        modelPortfolios = userService.currentUser().settings.modelPortfolioSettings.modelPortfolios
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

        val existingDefinitions =  portfolioService
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
                req.newInvestment?.let { newInvestments ->
                    val portfolio = Portfolio(
                            id = "new-portfolio",
                            name = "New Portfolio",
                            // position line item representing the new investment
                            positions = listOf(Position(id = "CASH", assetId = "USD", quantity = newInvestments))
                    )
                    val whiteList = whiteListResolver.resolveWhiteListItems(ResolveWhiteListItemRequest(portfolio, modelPortfolio)).whiteListItems
                    PortfolioDefinition(
                            portfolio = portfolio,
                            whiteList = whiteList
                    )
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
