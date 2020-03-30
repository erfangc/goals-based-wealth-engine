package io.github.erfangc.portfolios.models

import io.github.erfangc.assets.models.Asset

data class ImportPortfolioResponse(
        /**
         * The parsed results
         */
        val positionRows: List<PositionRow>,
        /**
         * A map of the resolved assets
         */
        val assets: Map<String, Asset>,
        /**
         * If the provided values are weights only, we will subsequently need the NAV of the imported portfolio
         */
        val requiresNavForScaling: Boolean = false
)