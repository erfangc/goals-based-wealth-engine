package io.github.erfangc.convexoptimizer.models

interface ConvexOptimizationRequest {
    val portfolios: List<PortfolioDefinition>
}