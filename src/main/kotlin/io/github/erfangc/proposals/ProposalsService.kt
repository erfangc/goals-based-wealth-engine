package io.github.erfangc.proposals

import io.github.erfangc.common.ErrorUtils.badInput
import io.github.erfangc.convexoptimizer.ConvexOptimizerService
import io.github.erfangc.convexoptimizer.models.*
import io.github.erfangc.goalsengine.ClientGoalsTranslatorService
import io.github.erfangc.goalsengine.GoalsEngineService
import io.github.erfangc.goalsengine.models.*
import io.github.erfangc.portfolios.PortfolioService
import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.portfolios.models.Position
import io.github.erfangc.proposals.internal.WhiteListResolver
import io.github.erfangc.proposals.models.GenerateProposalRequest
import io.github.erfangc.proposals.models.GenerateProposalResponse
import io.github.erfangc.proposals.models.Proposal
import io.github.erfangc.proposals.models.ResolveWhiteListItemRequest
import io.github.erfangc.users.UserService
import io.github.erfangc.users.models.ModelPortfolio
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.Instant
import java.util.*

@Service
class ProposalsService(
        private val userService: UserService,
        private val goalsEngineService: GoalsEngineService,
        private val portfolioService: PortfolioService,
        private val clientGoalsTranslatorService: ClientGoalsTranslatorService,
        private val convexOptimizerService: ConvexOptimizerService
) {

    private val log = LoggerFactory.getLogger(ProposalsService::class.java)

    /**
     * Initiate the workflow of creating a proposal
     *
     * The proposal workflow have a few possible steps it might take:
     *
     *  1 - if the client has not been assigned a model portfolio, and that model portfolio assignment is not enabled
     *  the efficient frontier will be used to determine optimal allocation
     */
    fun generateProposal(req: GenerateProposalRequest): GenerateProposalResponse {

        // resolve portfolios in-scope for this request
        val portfolioDefinitions = portfolioDefinitions(req)
        val portfolios = portfolioDefinitions.map { it.portfolio }

        val (modelPortfolio, optimizationResponse) = when (req.client.goals?.approach) {
            "model portfolio" -> {
                // if the client has an assigned model portfolio, do not run goals optimization, instead just run a probability
                // analysis
                val modelPortfolios = modelPortfolios()
                if (req.client.goals.autoAssignModelPortfolio == true) {
                    // automatically choose a model portfolio
                    val goalsOutput = modelPortfoliosBasedGoalsOptimization(req, portfolios, modelPortfolios)
                    goalsOutput.modelPortfolio to constrainedTrackingErrorOptimization(goalsOutput, req)
                } else {
                    val modelPortfolio = modelPortfolios.find { it.id == req.client.modelPortfolioId }
                            ?: badInput("Model ${req.client.modelPortfolioId} does not appear to be defined in your settings")
                    // do not run simulation use the probability engine to derive the probability
                    // of reaching the client's goals
                    val goalsOutput = modelPortfoliosBasedGoalsOptimization(req, portfolios, listOf(modelPortfolio))
                    modelPortfolio to constrainedTrackingErrorOptimization(goalsOutput, req)
                }
            }
            else -> {
                // use efficient frontier
                val goalsOutput = efficientFrontierBasedGoalsOptimization(req, portfolios)
                null to constrainedMeanVarianceOptimization(goalsOutput, req)
            }
        }

        val proposal = Proposal(
                id = UUID.randomUUID().toString(),
                portfolios = portfolios,
                proposedOrders = optimizationResponse.proposedOrders,
                modelPortfolio = modelPortfolio,
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString(),
                clientId = req.client.id
        )

        return GenerateProposalResponse(proposal = proposal)
    }

    private fun modelPortfolios(): List<ModelPortfolio> {
        return userService
                .currentUser()
                .settings
                .modelPortfolioSettings
                .modelPortfolios
    }

    private fun modelPortfoliosBasedGoalsOptimization(req: GenerateProposalRequest,
                                                      portfolios: List<Portfolio>,
                                                      modelPortfolios: List<ModelPortfolio>): ModelPortfolioBasedGoalsOptimizationResponse {
        val stopWatch = StopWatch()
        stopWatch.start()
        log.info("Running goals engine to figure out the best model portfolio for ${req.client.id}")
        val (goal, initialInvestment, cashflows, investmentHorizon) = clientGoalsTranslatorService
                .translate(TranslateClientGoalsRequest(client = req.client, portfolios = portfolios))
        val goalsOutput = goalsEngineService.modelPortfolioBasedGoalsOptimization(
                ModelPortfolioBasedGoalsOptimizationRequest(
                        initialWealth = initialInvestment,
                        cashflows = cashflows,
                        investmentHorizon = investmentHorizon,
                        goal = goal,
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

    private fun efficientFrontierBasedGoalsOptimization(req: GenerateProposalRequest, portfolios: List<Portfolio>): EfficientFrontierBasedGoalsOptimizationResponse {
        val stopWatch = StopWatch()
        log.info("Running goals engine to figure out the best risk reward for ${req.client.id}")
        val whiteListItems = userService.currentUser().settings.whiteList
        val (goal, initialInvestment, cashflows, investmentHorizon) = clientGoalsTranslatorService
                .translate(TranslateClientGoalsRequest(req.client, portfolios))
        stopWatch.start()
        val goalsOutput = goalsEngineService.efficientFrontierBasedGoalsOptimization(
                EfficientFrontierBasedGoalsOptimizationRequest(
                        initialWealth = initialInvestment,
                        cashflows = cashflows,
                        investmentHorizon = investmentHorizon,
                        goal = goal,
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
                .getPortfoliosForClient(req.client.id)
                .portfolios
                .map { portfolio ->
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
        val ret = (existingDefinitions + newPortfolio)
                // get rid of any portfolios that might not have a position
                .filter { it.portfolio.positions.isNotEmpty() }
        if (ret.isEmpty()) {
            badInput("Unable to initiate optimization since the client has no existing holdings nor putting in new investments")
        } else {
            return ret
        }
    }

}
