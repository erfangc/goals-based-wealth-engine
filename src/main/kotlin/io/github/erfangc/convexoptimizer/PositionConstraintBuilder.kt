package io.github.erfangc.convexoptimizer

import ilog.concert.IloConstraint
import org.slf4j.LoggerFactory

object PositionConstraintBuilder {

    private val log = LoggerFactory.getLogger(PositionConstraintBuilder::class.java)

    fun positionConstraints(ctx: OptimizationContext): List<IloConstraint> {
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
        log.info("Created ${positionMustSumToAssetConstraints.size} position constraints across " +
                "${ctx.assetIds.size} assets and " +
                "${ctx.portfolioDefinitions.size} portfolios")
        return positionMustSumToAssetConstraints
    }

}