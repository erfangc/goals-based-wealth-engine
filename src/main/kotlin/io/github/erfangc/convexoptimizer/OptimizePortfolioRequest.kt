package io.github.erfangc.convexoptimizer

import io.github.erfangc.portfolios.Portfolio

data class OptimizePortfolioRequest(val portfolio: Portfolio? = null, val targetExpectedReturn: Double)