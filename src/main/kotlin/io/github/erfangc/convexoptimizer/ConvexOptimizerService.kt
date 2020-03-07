package io.github.erfangc.convexoptimizer

import ilog.concert.IloNumExpr
import ilog.concert.IloNumVar
import ilog.concert.IloObjectiveSense
import ilog.cplex.IloCplex
import io.github.erfangc.assets.Asset
import io.github.erfangc.assets.AssetService
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.users.UserService
import org.springframework.stereotype.Service

@Service
class ConvexOptimizerService(
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
        private val assetService: AssetService,
        private val userService: UserService
) {

    internal class OptimizationContext(
            val cplex: IloCplex,
            val req: OptimizePortfolioRequest,
            val assets: Map<String, Asset>,
            val assetIds: List<String>,
            val assetVars: Map<String, IloNumVar>,
            val expectedReturns: Map<String, Double>
    )

    /**
     * For convex optimization there are two classes of objectives:
     *
     * 1 - We create the objective as minimize
     * portfolio risk given a variance covariance matrix
     *
     * Furthermore, we target an expected level of returns
     *
     * 2 - If a target (model) portfolio is provided, we minimize tracking error
     * to that of the model portfolio
     *
     */
    fun optimizePortfolio(req: OptimizePortfolioRequest): OptimizePortfolioResponse {

        val ctx = optimizationContext(req)
        val portfolios = req.portfolios?.map {
            portfolioDefinition -> portfolioDefinition.portfolio
            // figure out how to create position trading variables that must tie back to the asset variables
        }

        val cplex = ctx.cplex
        cplex.objective(IloObjectiveSense.Minimize, varianceExpr(ctx))
        // total weight must be 100%
        cplex.add(cplex.eq(1.0, cplex.sum(ctx.assetVars.values.toTypedArray())))
        // the resulting portfolio must target the level of return
        cplex.add(cplex.eq(returnExpr(ctx), req.objectives.expectedReturn))

        val solved = cplex.solve()
        if (!solved) {
            throw RuntimeException("The convex optimization problem cannot be solved")
        }

        //
        // position level restrictions (tax etc.)
        //
        TODO()
    }

    private fun varianceExpr(ctx: OptimizationContext): IloNumExpr? {
        val cplex = ctx.cplex
        //
        // build the risk terms on which we minimize
        //
        val response = covarianceService.computeCovariances(ctx.assetIds)
        val covariances = response.covariances
        val assetIndexLookup = response.assetIndexLookup
        val varianceTerms = ctx.assetIds.flatMap { a1 ->
            ctx.assetIds.map { a2 ->
                val a1Idx = assetIndexLookup[a1] ?: error("")
                val a2Idx = assetIndexLookup[a2] ?: error("")
                val covariance = covariances[a1Idx][a2Idx]
                val a1Var = ctx.assetVars[a1] ?: error("")
                val a2Var = ctx.assetVars[a2] ?: error("")
                cplex.prod(a1Var, a2Var, covariance)
            }
        }.toTypedArray()
        return cplex.sum(varianceTerms)
    }

    private fun returnExpr(ctx: OptimizationContext): IloNumExpr? {
        val cplex = ctx.cplex
        val returnExpr = ctx.assetIds.map { assetId ->
            val assetVar = ctx.assetVars[assetId] ?: error("")
            val expectedReturn = ctx.expectedReturns[assetId] ?: error("")
            cplex.prod(assetVar, expectedReturn)
        }.toTypedArray()
        return cplex.sum(returnExpr)
    }

    private fun optimizationContext(req: OptimizePortfolioRequest, cplex: IloCplex = IloCplex()): OptimizationContext {
        val assetIds = assetIds(req)
        val assets = assetService.getAssets(assetIds).associateBy { it.assetId }

        // each asset is an decision variable
        val assetVars = assetIds.map { assetId -> assetId to cplex.numVar(0.0, 1.0, assetId) }.toMap()
        val expectedReturns = expectedReturnsService.getExpectedReturns(assetIds)

        return OptimizationContext(cplex, req, assets, assetIds, assetVars, expectedReturns)
    }

    private fun assetIds(req: OptimizePortfolioRequest): List<String> {
        val holdings = holdingAssetIds(req)
        val whiteList = whiteList()
        return (holdings + whiteList).distinct()
    }

    private fun whiteList(): List<String> {
        val user = userService.getUser()
        return user.overrides?.whiteList?.map { it.assetId } ?: emptyList()
    }

    private fun holdingAssetIds(req: OptimizePortfolioRequest): List<String> {
        return req
                .portfolios
                ?.flatMap { it.portfolio.positions.map { position -> position.assetId } }
                ?: emptyList()
    }

}