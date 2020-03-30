package io.github.erfangc.convexoptimizer.models

data class ConstrainedMeanVarianceOptimizationRequest(
        override val portfolios: List<PortfolioDefinition>,
        val objectives: Objectives
) : ConvexOptimizationRequest

