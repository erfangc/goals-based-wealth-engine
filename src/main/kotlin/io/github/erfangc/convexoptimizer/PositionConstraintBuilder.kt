package io.github.erfangc.convexoptimizer

import ilog.concert.IloConstraint
import org.slf4j.LoggerFactory

object PositionConstraintBuilder {

    private val log = LoggerFactory.getLogger(PositionConstraintBuilder::class.java)

    /**
     * Build position constraints
     * Reminder that we have two classes of decision variables (by inspection of the optimization context)
     *
     * 1 - Asset decision variables, i.e. AAPL + VTI + AGG
     * 2 - Position decision variables, i.e. (AAPL in Port A), (VTI bought 10 days ago)
     *
     * The position decision variables must sum to the asset decision variables to make the problem internally consistent
     * i.e. the position variables are used to create constraints such that the position decision variables
     * when summed must equal to their corresponding asset variables. (ex: all position decision variable AAPL in all portfolios must = the asset AAPL variable)
     */
    fun positionConstraints(ctx: OptimizationContext): List<IloConstraint> {
        val cplex = ctx.cplex
        val analyses = ctx.marketValueAnalyses
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
                        val netAssetValue = analyses.netAssetValues[portfolioId] ?: 0.0
                        val posWt = analyses.weights[portfolioId]?.get(positionId) ?: 0.0
                        val portWt = netAssetValue / aggregateNav
                        val originalWtToAgg = portWt * posWt
                        // we use sum here b/c the numVar represent a % weight of the
                        // aggregateNAV to transact
                        cplex.sum(positionVar.numVar, originalWtToAgg)
                    }.toTypedArray()
                    cplex.eq(cplex.sum(terms), assetVar, "weight of all positions in $assetId must be the weight of $assetId")
                }
        log.info("Created ${positionMustSumToAssetConstraints.size} position constraints across " +
                "${ctx.assetIds.size} assets and " +
                "${ctx.portfolioDefinitions.size} portfolios")
        return positionMustSumToAssetConstraints
    }

}