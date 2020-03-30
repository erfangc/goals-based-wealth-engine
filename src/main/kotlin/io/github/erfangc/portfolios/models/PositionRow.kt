package io.github.erfangc.portfolios.models

import io.github.erfangc.assets.models.Asset

data class PositionRow(
        val asset: Asset? = null,
        val position: Position? = null,
        val error: ResolvePortfolioError? = null
)