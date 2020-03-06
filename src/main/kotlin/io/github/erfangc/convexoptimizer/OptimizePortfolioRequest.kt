package io.github.erfangc.convexoptimizer

import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.users.WhiteListItem

data class PortfolioDefinition(val portfolio: Portfolio,
                               val withdrawRestricted: Boolean = false,
                               val whiteList: List<WhiteListItem>? = null)

data class Objectives(val expectedReturn: Double)

data class OptimizePortfolioRequest(val portfolios: List<PortfolioDefinition>? = null,
                                    val objectives: Objectives)