package io.github.erfangc.util

import io.github.erfangc.portfolios.Portfolio

object PortfolioUtils {
    fun assetIds(portfolios: List<Portfolio>): List<String> {
        return portfolios.flatMap { it.positions.map { pos -> pos.assetId } }.distinct()
    }
}