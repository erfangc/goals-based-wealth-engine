package io.github.erfangc.convexoptimizer

import io.github.erfangc.users.settings.ModelPortfolio

data class ConstrainedTrackingErrorOptimizationRequest(
        override val portfolios: List<PortfolioDefinition>,
        val modelPortfolio: ModelPortfolio
) : ConvexOptimizationRequest