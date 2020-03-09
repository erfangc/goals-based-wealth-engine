package io.github.erfangc.convexoptimizer

import io.github.erfangc.assets.Asset
import io.github.erfangc.portfolios.Position
import kotlin.math.floor

object SolutionParser {

    /**
     * Parse through all the decision variables that represents suggested trading
     * in different positions across different portfolios
     */
    fun parseSolution(ctx: OptimizationContext): OptimizePortfolioResponse {
        val proposedOrders = ctx
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
                        ProposedOrder(
                                portfolioId = portfolioId,
                                assetId = assetId,
                                positionId = position.id,
                                quantity = quantity(targetMv, ctx.assets[assetId])
                        )
                    }
                }
        val orders = proposedOrders.groupBy { it.portfolioId }.mapValues { it.value.associateBy { order -> order.positionId } }
        val portfolioDefinitions = ctx.portfolioDefinitions

        // overlay the existing portfolio definitions (which includes any new portfolio created) with orders
        val proposedPortfolios = portfolioDefinitions.map { portfolioDefinition ->
            val portfolio = portfolioDefinition.portfolio
            val portfolioId = portfolio.id
            val portfolioOrders = orders[portfolioId] ?: emptyMap()
            val updatedExistingPositions = portfolio.positions.map { position ->
                val positionId = position.id
                val order = portfolioOrders[positionId] ?: error("")
                position.copy(quantity = position.quantity + order.quantity)
            }
            // append any new positions
            val newPositions = portfolioOrders.keys.subtract(updatedExistingPositions.map { it.id }).map { positionId ->
                val order = portfolioOrders[positionId] ?: error("")
                Position(
                        id = positionId,
                        quantity = order.quantity,
                        assetId = order.assetId
                )
            }
            portfolio.copy(positions = updatedExistingPositions + newPositions)
        }
        return OptimizePortfolioResponse(proposedPortfolios = proposedPortfolios, proposedOrders = proposedOrders)
    }

    private fun quantity(targetMv: Double, asset: Asset?): Double {
        return floor(targetMv / (asset?.price ?: 0.0))
    }

}