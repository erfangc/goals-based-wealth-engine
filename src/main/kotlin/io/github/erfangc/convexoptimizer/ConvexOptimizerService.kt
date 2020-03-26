package io.github.erfangc.convexoptimizer

import ilog.concert.IloNumExpr
import ilog.concert.IloObjectiveSense
import ilog.concert.IloRange
import ilog.cplex.IloCplex
import io.github.erfangc.analysis.AnalysisRequest
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.assets.AssetService
import io.github.erfangc.convexoptimizer.PositionConstraintBuilder.positionConstraints
import io.github.erfangc.convexoptimizer.PositionVariablesFactory.positionVars
import io.github.erfangc.convexoptimizer.SolutionParser.parseSolution
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysis
import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.portfolios.Position
import io.github.erfangc.users.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * ConvexOptimizerService performs build portfolios or modify existing portfolios
 * that results in specific recommended transactions
 */
@Service
class ConvexOptimizerService(
        private val analysisService: AnalysisService,
        private val expectedReturnsService: ExpectedReturnsService,
        covarianceService: CovarianceService,
        private val assetService: AssetService,
        private val userService: UserService
) {

    private val log = LoggerFactory.getLogger(ConvexOptimizerService::class.java)

    private val varianceExpressionBuilder = VarianceExpressionBuilder(covarianceService, analysisService)

    /**
     *
     * For convex optimization there are two classes of objectives:
     *
     * 1 - We create the objective as minimize
     * portfolio risk given a variance covariance matrix
     *
     * Furthermore, we target an expected level of returns
     *
     * 2 - If a target (model) portfolio is provided, we minimize tracking error
     * to that of the model portfolio (not done yet)
     *
     */
    fun optimizePortfolio(req: OptimizePortfolioRequest): OptimizePortfolioResponse {

        log.info("Beginning convex optimization for ${req.portfolios?.size ?: 0} portfolios")

        // create an OptimizationContext instance to keep track of reusable references like
        // the decision variables, assets lookup etc.
        val ctx = optimizationContext(req)

        log.info(
                "Built OptimizationContext for convex optimization," +
                        " assetVars=${ctx.assetVars.size}," +
                        " positionVars=${ctx.positionVars.size}," +
                        " aggregateNav=${ctx.aggregateNav}"
        )

        val cplex = ctx.cplex

        // create the objective which is to minimize risk (our constraint is to target return)
        cplex.addObjective(IloObjectiveSense.Minimize, varianceExpressionBuilder.varianceExpr(ctx))

        // total weight must be 100%
        cplex.add(cplex.eq(1.0, cplex.sum(ctx.assetVars.values.toTypedArray()), "weight of all assets must equal 1.0"))

        // the resulting portfolio must target the level of return
        cplex.add(cplex.ge(returnExpr(ctx), req.objectives.expectedReturn, "expected return must equal ${req.objectives.expectedReturn}"))

        // position level restrictions
        positionConstraints(ctx).forEach { constraint ->
            cplex.add(constraint)
        }

        // for any portfolio whose NAV must stay constant, we forbid transfer by fixing
        // sum of position to original proportion to aggregate NAV
        val portfolioConstraints = portfolioConstraint(ctx)
        portfolioConstraints.forEach { constraint -> cplex.add(constraint) }

        if (!cplex.solve()) {
            log.error("Unable to solve the convex optimization problem cplex.status=${cplex.status}")
            throw RuntimeException("The convex optimization problem cannot be solved")
        }

        // parse the solution back into a portfolio / orders
        return parseSolution(ctx)
    }

    private fun portfolioConstraint(ctx: OptimizationContext): List<IloRange> {
        val marketValueAnalyses = ctx.marketValueAnalyses
        val nav = marketValueAnalyses.netAssetValues
        val aggNav = marketValueAnalyses.netAssetValue
        return ctx.portfolioDefinitions
                .filter { it.withdrawRestricted }
                .map { portfolioDefinition ->
                    val portfolioId = portfolioDefinition.portfolio.id
                    val portfolioWt = (nav[portfolioId] ?: 0.0) / aggNav
                    val weights = marketValueAnalyses.weights[portfolioId]
                    val terms = ctx
                            .positionVars
                            .filter { it.portfolioId == portfolioId }
                            .map {
                                val positionId = it.position.id
                                val positionWt = weights?.get(positionId) ?: 0.0
                                // original weight
                                val originalWt = positionWt * portfolioWt
                                ctx.cplex.sum(originalWt, it.numVar)
                            }
                            .toTypedArray()
                    ctx.cplex.eq(
                            ctx.cplex.sum(terms),
                            portfolioWt,
                            "${portfolioDefinition.portfolio.name} weight must be $portfolioWt"
                    )
                }
    }

    private fun marketValueAnalysis(portfolioDefinitions: List<PortfolioDefinition>): MarketValueAnalysis {
        return analysisService
                .analyze(AnalysisRequest(portfolioDefinitions.map { it.portfolio }))
                .analysis
                .marketValueAnalysis
    }

    /**
     * Construct the CPLEX expression that represent
     * expected returns as a function of asset weights
     */
    private fun returnExpr(ctx: OptimizationContext): IloNumExpr {
        val cplex = ctx.cplex
        val returnExpr = ctx.assetIds.map { assetId ->
            val assetVar = ctx.assetVars[assetId] ?: error("")
            val expectedReturn = ctx.expectedReturns[assetId] ?: error("")
            cplex.prod(assetVar, expectedReturn)
        }.toTypedArray()
        return cplex.sum(returnExpr)
    }

    private fun optimizationContext(req: OptimizePortfolioRequest,
                                    cplex: IloCplex = IloCplex()): OptimizationContext {
        // from the market value analyse we can formulate position level constraints
        val portfolios = (
                (req.portfolios ?: emptyList()) +
                        listOfNotNull(
                                req.newInvestments?.let { newInvestments ->
                                    PortfolioDefinition(
                                            portfolio = Portfolio(
                                                    id = "new-portfolio",
                                                    name = "New Portfolio",
                                                    positions = listOf(
                                                            Position(id = "CASH", assetId = "USD", quantity = newInvestments)
                                                    )
                                            )
                                    )
                                }
                        )
                )
                // get rid of any portfolios that might not have a position
                .filter { it.portfolio.positions.isNotEmpty() }

        if (portfolios.isEmpty()) {
            // it is not possible to construct a portfolio
            // without existing portfolios or an new amount to invest
            throw IllegalStateException("Unable to proceed with convex optimization without either existing portfolios or a new investment amount")
        }

        val assetIds = assetIds(req)
        val assets = assetService.getAssets(assetIds).associateBy { it.id }

        // each asset is an decision variable
        val assetVars = assetIds.map { assetId -> assetId to cplex.numVar(0.0, 1.0, assetId) }.toMap()
        val expectedReturns = expectedReturnsService.getExpectedReturns(assetIds)
        val marketValueAnalysis = marketValueAnalysis(portfolios)

        // create the actual position variables (this method is a bit ugly, as it accepts many arguments, however it gets the job done)
        val positionVars = positionVars(
                portfolios,
                cplex,
                marketValueAnalysis,
                userService.currentUser().settings?.whiteList
        )


        // aggregate the NAV of all portfolios so we can find out how much a position
        // is weighted in the broader aggregated portfolio
        val aggregateNav = marketValueAnalysis.netAssetValue

        return OptimizationContext(
                cplex,
                req,
                assets,
                assetIds,
                assetVars,
                portfolios,
                positionVars,
                expectedReturns,
                marketValueAnalysis,
                aggregateNav
        )
    }

    /**
     * This is the comprehensive set of assets the user is allowed to hold
     * regardless of whether the asset is part of existing hold or part of the white list
     */
    private fun assetIds(req: OptimizePortfolioRequest): List<String> {
        val holdings = holdingAssetIds(req)
        val user = userService.currentUser()
        val whiteList = user.settings?.whiteList?.map { it.assetId } ?: emptyList()
        return (holdings + whiteList + "USD").distinct()
    }

    private fun holdingAssetIds(req: OptimizePortfolioRequest): List<String> {
        return req
                .portfolios
                ?.flatMap { it.portfolio.positions.map { position -> position.assetId } }
                ?: emptyList()
    }

}