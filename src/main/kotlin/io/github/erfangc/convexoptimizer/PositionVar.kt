package io.github.erfangc.convexoptimizer

import ilog.concert.IloNumVar
import io.github.erfangc.portfolios.Position

/**
 * This data class denotes a decision variable for how much to transact
 * in a given position
 *
 * These decision variables of course must sum up to the appropriate allocation to the assets
 *
 * @param numVar this is the weight to trade in the given position
 */
data class PositionVar(
        val id: String,
        val portfolioId: String,
        val position: Position,
        val numVar: IloNumVar
)