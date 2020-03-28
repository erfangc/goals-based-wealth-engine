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
     * Performs an convex optimization by minimizing the tracking error between the provided set of portfolios
     * and the model portfolio
     */
    fun constrainedTrackingErrorOptimization(req: ConstrainedTrackingErrorOptimizationRequest): ConvexOptimizationResponse {

        log.info("Beginning constrained tracking error optimization for ${req.portfolios.size} portfolios")

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
        cplex.addObjective(
                IloObjectiveSense.Minimize, varianceExpressionBuilder
                .exprForTrackingError(req.modelPortfolio.portfolio, ctx)
        )

        // total weight must be 100%
        cplex.add(cplex.eq(1.0, cplex.sum(ctx.assetVars.values.toTypedArray()), "weight of all assets must equal 1.0"))

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

    /**
     *
     */
    fun constrainedMeanVarianceOptimization(req: ConstrainedMeanVarianceOptimizationRequest): ConvexOptimizationResponse {

        log.info("Beginning constrained mean variance optimization for ${req.portfolios.size} portfolios")

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
        cplex.addObjective(IloObjectiveSense.Minimize, varianceExpressionBuilder.exprForVariance(ctx))

        // total weight must be 100%
        cplex.add(cplex.eq(1.0, cplex.sum(ctx.assetVars.values.toTypedArray()), "weight of all assets must equal 1.0"))

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

    private fun optimizationContext(req: ConvexOptimizationRequest,
                                    cplex: IloCplex = IloCplex()): OptimizationContext {

        val assetIdsInPosition = assetIdsInPosition(req)
        val whiteListAssetIds = whiteListAssetIds(req)
        log.info("Found ${assetIdsInPosition.size} distinct assets in position, ${whiteListAssetIds.size} distinct assets on the white list")
        val assetIds = (assetIdsInPosition + whiteListAssetIds + "USD").distinct()
        log.info("Found ${assetIds.size} distinct assets in in total, these are the asset level decision variables")
        val assets = assetService.getAssets(assetIds).associateBy { it.id }

        // each asset is an decision variable
        val assetVars = assetIds.map { assetId -> assetId to cplex.numVar(0.0, 1.0, assetId) }.toMap()
        val expectedReturns = expectedReturnsService.getExpectedReturns(assetIds)
        val marketValueAnalysis = marketValueAnalysis(req.portfolios)

        // create the actual position variables (this method is a bit ugly, as it accepts many arguments, however it gets the job done)
        val positionVars = positionVars(
                req.portfolios,
                cplex,
                marketValueAnalysis
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
                req.portfolios,
                positionVars,
                expectedReturns,
                marketValueAnalysis,
                aggregateNav
        )
    }

    private fun assetIdsInPosition(req: ConvexOptimizationRequest): List<String> {
        return req
                .portfolios
                .flatMap { it.portfolio.positions.map { position -> position.assetId } }
                .distinct()
    }

    private fun whiteListAssetIds(req: ConvexOptimizationRequest): List<String> {
        return req
                .portfolios
                .flatMap { it.whiteList.map { item -> item.assetId } }
                .distinct()
    }

}