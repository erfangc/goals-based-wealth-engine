package io.github.erfangc.portfolios.dataimport

import io.github.erfangc.assets.Asset
import io.github.erfangc.portfolios.Portfolio

data class ResolvePortfolioResponse(
        /**
         * The parsed results
         */
        val parsedRows: List<ParsedRow>,
        /**
         * The parsed portfolio. If only weights are provided, the NAV is assumed 1_000_000
         */
        val portfolio: Portfolio,
        /**
         * Any errors encountered during parsing the portfolio
         */
        val errors: List<ResolvePortfolioError>,
        /**
         * A map of the resolved assets
         */
        val assets: Map<String, Asset>,
        /**
         * If the provided values are weights only, we will subsequently need the NAV of the imported portfolio
         */
        val requiresNavForScaling: Boolean = false
)