package io.github.erfangc.convexoptimizer

import ilog.concert.IloNumExpr
import ilog.concert.IloObjectiveSense
import ilog.cplex.IloCplex
import io.github.erfangc.assets.AssetService
import io.github.erfangc.convexoptimizer.PositionConstraintBuilder.positionConstraints
import io.github.erfangc.convexoptimizer.PositionVariablesFactory.positionVars
import io.github.erfangc.convexoptimizer.SolutionParser.parseSolution
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysis
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
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
        private val marketValueAnalysisService: MarketValueAnalysisService,
        private val expectedReturnsService: ExpectedReturnsService,
        covarianceService: CovarianceService,
        private val assetService: AssetService,
        private val userService: UserService
) {

    private val log = LoggerFactory.getLogger(ConvexOptimizerService::class.java)

    private val varianceExpressionBuilder = VarianceExpressionBuilder(covarianceService)

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
        cplex.add(cplex.eq(1.0, cplex.sum(ctx.assetVars.values.toTypedArray())))

        // the resulting portfolio must target the level of return
        cplex.add(cplex.ge(returnExpr(ctx), req.objectives.expectedReturn))

        // position level restrictions
        positionConstraints(ctx).forEach { constraint ->
            cplex.add(constraint)
        }

        if (!cplex.solve()) {
            log.error("Unable to solve the convex optimization problem cplex.status=${cplex.status}")
            throw RuntimeException("The convex optimization problem cannot be solved")
        }

        // parse the solution back into a portfolio / orders
        return parseSolution(ctx)
    }

    private fun analyses(portfolioDefinitions: List<PortfolioDefinition>): Map<String, MarketValueAnalysis> {
        return portfolioDefinitions.map { portfolioDefinition ->
            // figure out how to create position trading
            // variables that must tie back to the asset variables
            val analysis = marketValueAnalysisService
                    .marketValueAnalysis(MarketValueAnalysisRequest(portfolioDefinition.portfolio))
                    .marketValueAnalysis
            portfolioDefinition.portfolio.id to analysis
        }.toMap()
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
        val portfolios = (req.portfolios ?: emptyList()) + listOfNotNull(
                req.newInvestments?.let { newInvestments ->
                    PortfolioDefinition(
                            portfolio = Portfolio(
                                    id = "new-portfolio",
                                    positions = listOf(
                                            Position(id = "CASH", assetId = "USD", quantity = newInvestments)
                                    )
                            )
                    )
                }
        )

        if (portfolios.isEmpty()) {
            // it is not possible to construct a portfolio
            // without existing portfolios or an new amount to invest
            throw IllegalStateException("Unable to proceed with convex optimization without either existing portfolios or a new investment amount")
        }

        val assetIds = assetIds(req)
        val assets = assetService.getAssets(assetIds).associateBy { it.assetId }

        // each asset is an decision variable
        val assetVars = assetIds.map { assetId -> assetId to cplex.numVar(0.0, 1.0, assetId) }.toMap()
        val expectedReturns = expectedReturnsService.getExpectedReturns(assetIds)

        // create the actual position variables
        val positionVars = positionVars(portfolios, cplex, userService.getUser().overrides?.whiteList)

        val analyses = analyses(portfolios)

        // aggregate the NAV of all portfolios so we can find out how much a position
        // is weighted in the broader aggregated portfolio
        val aggregateNav = analyses
                .values
                .fold(0.0) { acc, analysis -> acc.plus(analysis.netAssetValue) }

        return OptimizationContext(
                cplex,
                req,
                assets,
                assetIds,
                assetVars,
                portfolios,
                positionVars,
                expectedReturns,
                analyses,
                aggregateNav
        )
    }

    private fun assetIds(req: OptimizePortfolioRequest): List<String> {
        val holdings = holdingAssetIds(req)
        val user = userService.getUser()
        val whiteList = user.overrides?.whiteList?.map { it.assetId } ?: emptyList()
        return (holdings + whiteList + "USD").distinct()
    }

    private fun holdingAssetIds(req: OptimizePortfolioRequest): List<String> {
        return req
                .portfolios
                ?.flatMap { it.portfolio.positions.map { position -> position.assetId } }
                ?: emptyList()
    }

}