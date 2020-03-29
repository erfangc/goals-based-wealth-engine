package io.github.erfangc.convexoptimizer

import io.github.erfangc.assets.Asset
import io.github.erfangc.portfolios.Position
import kotlin.math.floor

object SolutionParser {

    /**
     * Parse through all the decision variables that represents suggested trading
     * in different positions across different portfolios
     */
    fun parseSolution(ctx: OptimizationContext): ConvexOptimizationResponse {
        val proposedOrders = ctx
                .positionVars
                .groupBy { it.portfolioId }
                .flatMap { (portfolioId, positionVars) ->
                    positionVars
                            .map {
                                // the numVar are tradeWeight to the aggregate
                                positionVar ->
                                val aggWt = ctx.cplex.getValue(positionVar.numVar)
                                val targetMv = ctx.aggregateNav * aggWt
                                val position = positionVar.position
                                val assetId = position.assetId
                                ProposedOrder(
                                        portfolioId = portfolioId,
                                        assetId = assetId,
                                        positionId = position.id,
                                        // ensure cash get treated as if price = 1
                                        quantity = if (assetId == "USD") {
                                            targetMv
                                        } else {
                                            quantity(targetMv, ctx.assets[assetId] ?: error(""))
                                        }
                                )
                            }
                }
                .filter { it.quantity != 0.0 }
        // overlay the existing portfolio definitions (which includes any new portfolio created) with orders
        return ConvexOptimizationResponse(proposedOrders = proposedOrders)
    }

    private fun quantity(targetMv: Double, asset: Asset): Double {
        return floor(targetMv / (asset.price ?: 0.0))
    }
}