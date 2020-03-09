package io.github.erfangc.convexoptimizer

import ilog.cplex.IloCplex
import io.github.erfangc.portfolios.Position
import io.github.erfangc.users.WhiteListItem
import java.util.*

object PositionVariablesFactory {
    /**
     * Derive position level decision variables
     *
     * Mathematical expressions such as return / risk are agnostic to position level information such as
     * cost or which portfolio the exposure came from. In these cases, the mathematical expressions
     * are constructed by using the asset decision variables
     *
     * However, this still leaves us with the problem that the position level constraints must be captured, namely:
     *  - Ensuring positions in the same asset across different portfolios sum to the weight of the asset decision variable
     *  across all portfolios
     *  - Cost / taxes and transfer requirements are properly modeled
     *
     * This is why in addition to asset decision variables, we also define a set of decision variables for the positions
     */
    fun positionVars(portfolios: List<PortfolioDefinition>,
                             cplex: IloCplex,
                             defaultWhiteList: List<WhiteListItem>?): List<PositionVar> {
        return portfolios.flatMap { portfolioDefinition ->
            val portfolio = portfolioDefinition.portfolio
            val portfolioId = portfolio.id
            val existingPositionVars = portfolio.positions.map { position ->
                val positionId = position.id
                PositionVar(
                        id = "$portfolioId#$positionId",
                        portfolioId = portfolioId,
                        position = position,
                        numVar = cplex.numVar(-1.0, 0.0, "$portfolioId#$positionId")
                )
            }

            // use the white list from the portfolio itself if defined
            // i.e. this would be the case if the account is a 401K account with limited investment options
            val whiteList = defaultWhiteList ?: portfolioDefinition.whiteList
            val whiteListVars = whiteList?.map {
                whiteListItem ->
                val positionId = UUID.randomUUID().toString()
                val assetId = whiteListItem.assetId
                PositionVar(
                        id = "$portfolioId#$positionId",
                        numVar = cplex.numVar(0.0, 1.0, "$portfolioId#$positionId"),
                        position = Position(id = positionId, quantity = 0.0, assetId = assetId),
                        portfolioId = portfolioId
                )
            } ?: emptyList()
            existingPositionVars + whiteListVars
        }
    }

}