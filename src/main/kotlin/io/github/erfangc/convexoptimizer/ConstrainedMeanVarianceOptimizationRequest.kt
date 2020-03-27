package io.github.erfangc.convexoptimizer

data class ConstrainedMeanVarianceOptimizationRequest(
        override val portfolios: List<PortfolioDefinition>,
        val objectives: Objectives
) : ConvexOptimizationRequest

