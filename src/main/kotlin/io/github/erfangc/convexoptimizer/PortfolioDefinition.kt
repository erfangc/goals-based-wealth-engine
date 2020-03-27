package io.github.erfangc.convexoptimizer

import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.users.settings.WhiteListItem

/**
 * Represents a single portfolio in a multi-portfolio convex optimization
 * problem. This data class describe not only the positions of the portfolio
 * but any other metadata necessary to perform the correct convex optimization
 * solution such as any restrictions on the portfolio itself
 */
data class PortfolioDefinition(
        val portfolio: Portfolio,
        /**
         * Describes whether it is possible to remove funds from this portfolio
         * (i.e. a IRA / 401K account prohibits withdraws)
         */
        val withdrawRestricted: Boolean = false,
        /**
         * The list of investments available
         */
        val whiteList: List<WhiteListItem>
)