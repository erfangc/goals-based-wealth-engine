package io.github.erfangc.portfolios.dataimport

import io.github.erfangc.assets.Asset
import io.github.erfangc.portfolios.Position

data class ParsedRow(
        val asset: Asset? = null,
        val position: Position? = null,
        val error: ResolvePortfolioError? = null
)