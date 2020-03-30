package io.github.erfangc.convexoptimizer.models

import io.github.erfangc.users.models.ModelPortfolio

data class ConstrainedTrackingErrorOptimizationRequest(
        override val portfolios: List<PortfolioDefinition>,
        val modelPortfolio: ModelPortfolio
) : ConvexOptimizationRequest