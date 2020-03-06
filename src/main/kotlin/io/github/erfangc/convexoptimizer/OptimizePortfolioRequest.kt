package io.github.erfangc.convexoptimizer

data class OptimizePortfolioRequest(val portfolios: List<PortfolioDefinition>? = null, val objectives: Objectives)