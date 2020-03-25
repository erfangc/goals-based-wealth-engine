package io.github.erfangc.convexoptimizer

import io.github.erfangc.users.settings.ModelPortfolio

data class OptimizePortfolioRequest(val portfolios: List<PortfolioDefinition>? = null,
                                    val objectives: Objectives,
                                    val modelPortfolio: ModelPortfolio? = null,
                                    val newInvestments: Double? = null
)