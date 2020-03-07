package io.github.erfangc.convexoptimizer

import ilog.concert.IloConstraint
import ilog.concert.IloNumExpr
import ilog.concert.IloNumVar
import ilog.concert.IloObjectiveSense
import ilog.cplex.IloCplex
import io.github.erfangc.assets.Asset
import io.github.erfangc.assets.AssetService
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.portfolios.Position
import io.github.erfangc.users.UserService
import io.github.erfangc.util.MarketValueAnalysis
import io.github.erfangc.util.WeightComputer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.floor

@Service
class ConvexOptimizerService(
        private val weightComputer: WeightComputer,
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
        private val assetService: AssetService,
        private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(ConvexOptimizerService::class.java)

    internal data class OptimizationContext(
            val cplex: IloCplex,
            val request: OptimizePortfolioRequest,
            val assets: Map<String, Asset>,
            val assetIds: List<String>,
            val assetVars: Map<String, IloNumVar>,
            val portfolioDefinitions: List<PortfolioDefinition>,
            val positionVars: List<PositionVar>,
            val expectedReturns: Map<String, Double>,
            val analyses: Map<String, MarketValueAnalysis>,
            val aggregateNav: Double
    )

    /**
     * This data class denotes a decision variable for how much to transact
     * in a given position
     *
     * These decision variables of course must sum up to the appropriate allocation to the assets
     *
     * @param numVar this is the weight to trade in the given position
     */
    internal data class PositionVar(val id: String, val portfolioId: String, val position: Position, val numVar: IloNumVar)

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

        val cplex = ctx.cplex
        cplex.objective(IloObjectiveSense.Minimize, varianceExpr(ctx))

        // total weight must be 100%
        cplex.add(cplex.eq(1.0, cplex.sum(ctx.assetVars.values.toTypedArray())))

        // the resulting portfolio must target the level of return
        cplex.add(cplex.eq(returnExpr(ctx), req.objectives.expectedReturn))

        // position level restrictions
        positionConstraints(ctx).forEach { constraint ->
            cplex.add(constraint)
        }

        if (!cplex.solve()) {
            throw RuntimeException("The convex optimization problem cannot be solved")
        }

        // parse the solution back into a portfolio
        return OptimizePortfolioResponse(orders = parseOrders(ctx))
    }

    private fun parseOrders(ctx: OptimizationContext): List<Order> {
        return ctx
                .positionVars
                .groupBy { it.portfolioId }
                .flatMap { (portfolioId, positionVars) ->
                    positionVars.map {
                        // the numVar are tradeWeight to the aggregate
                        positionVar ->
                        val aggWt = ctx.cplex.getValue(positionVar.numVar)
                        val targetMv = ctx.aggregateNav * aggWt
                        val position = positionVar.position
                        val assetId = position.assetId
                        Order(
                                portfolioId = portfolioId,
                                assetId = assetId,
                                positionId = position.id,
                                quantity = quantity(targetMv, ctx.assets[assetId])
                        )
                    }
                }
    }

    private fun quantity(targetMv: Double, asset: Asset?): Double {
        return floor(targetMv / (asset?.price ?: 0.0))
    }

    private fun positionConstraints(ctx: OptimizationContext): List<IloConstraint> {
        val cplex = ctx.cplex
        val analyses = ctx.analyses
        val aggregateNav = ctx.aggregateNav

        // position variables must 1) when summed, be consistent within their portfolio 2) when summed across assets
        // be consistent with the allocation to that asset
        val positionMustSumToAssetConstraints = ctx.positionVars
                .groupBy { it.position.assetId }
                .map {
                    // each asset group should have position sum up to it
                    (assetId, vars) ->
                    val assetVar = ctx.assetVars[assetId]

                    val terms = vars.map { positionVar ->
                        val portfolioId = positionVar.portfolioId
                        val positionId = positionVar.position.id
                        // query the MarketValueAnalysis object for the position weight we've previously memoized
                        // this analysis also stores the NAV of the portfolio, from which we can derive
                        // the portfolio's weight to the aggregate
                        val mvAnalysis = analyses[portfolioId] ?: error("")
                        val portWt = mvAnalysis.netAssetValue / aggregateNav
                        val posWt = mvAnalysis.weights[positionId] ?: 0.0
                        val originalWtToAgg = portWt * posWt
                        // we use sum here b/c the numVar represent a % weight of the
                        // aggregateNAV to transact
                        cplex.sum(positionVar.numVar, originalWtToAgg)
                    }.toTypedArray()
                    cplex.eq(cplex.sum(terms), assetVar)
                }
        logger.info("Created ${positionMustSumToAssetConstraints.size} position constraints across " +
                "${ctx.assetIds.size} assets and " +
                "${ctx.portfolioDefinitions.size} portfolios")
        return positionMustSumToAssetConstraints
    }

    private fun analyses(portfolioDefinitions: List<PortfolioDefinition>): Map<String, MarketValueAnalysis> {
        return portfolioDefinitions.map { portfolioDefinition ->
            // figure out how to create position trading
            // variables that must tie back to the asset variables
            val analysis = weightComputer.marketValueAnalysis(portfolioDefinition.portfolio)
            portfolioDefinition.portfolio.id to analysis
        }.toMap()
    }

    private fun varianceExpr(ctx: OptimizationContext): IloNumExpr {
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
        val portfolios = req.portfolios ?: listOf(
                PortfolioDefinition(
                        portfolio = Portfolio(
                                id = "new-portfolio",
                                // TODO new portfolio should start with a pre-defined amount given by the request
                                positions = listOf(
                                        Position(id = "CASH", assetId = "USD", quantity = 1_000_000.0)
                                )
                        )
                )
        )
        val assetIds = assetIds(req)
        val assets = assetService.getAssets(assetIds).associateBy { it.assetId }

        // each asset is an decision variable
        val assetVars = assetIds.map { assetId -> assetId to cplex.numVar(0.0, 1.0, assetId) }.toMap()
        val expectedReturns = expectedReturnsService.getExpectedReturns(assetIds)

        // create the actual position variables
        val positionVars = positionVars(portfolios, cplex)

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

    private fun positionVars(portfolios: List<PortfolioDefinition>, cplex: IloCplex): List<PositionVar> {
        return portfolios.flatMap { portfolioDefinition ->
            val portfolio = portfolioDefinition.portfolio
            portfolio.positions.map { position ->
                val portfolioId = portfolio.id
                val positionId = position.id
                PositionVar(
                        id = "$portfolioId#$positionId",
                        portfolioId = portfolioId,
                        position = position,
                        numVar = cplex.numVar(0.0, 1.0, "$portfolioId#$positionId")
                )
            }
        }
    }

    private fun assetIds(req: OptimizePortfolioRequest): List<String> {
        val holdings = holdingAssetIds(req)
        val whiteList = defaultWhiteList()
        return (holdings + whiteList).distinct()
    }

    /**
     * Figure out the default white list for the current user
     */
    private fun defaultWhiteList(): List<String> {
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