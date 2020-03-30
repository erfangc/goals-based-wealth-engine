package io.github.erfangc.common

import io.github.erfangc.portfolios.models.Portfolio

object PortfolioUtils {
    fun assetIds(portfolios: List<Portfolio>): List<String> {
        return portfolios.flatMap { it.positions.map { pos -> pos.assetId } }.distinct()
    }
}