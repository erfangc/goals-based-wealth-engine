package io.github.erfangc.convexoptimizer

import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.users.settings.WhiteListItem

data class PortfolioDefinition(val portfolio: Portfolio,
                               val withdrawRestricted: Boolean = false,
                               val whiteList: List<WhiteListItem>? = null)