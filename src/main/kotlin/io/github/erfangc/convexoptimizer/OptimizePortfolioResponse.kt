package io.github.erfangc.convexoptimizer

import io.github.erfangc.portfolios.Portfolio

data class OptimizePortfolioResponse(
        val proposedPortfolios: List<Portfolio>,
        val originalPortfolios: List<Portfolio>,
        val proposedOrders: List<ProposedOrder>
)
