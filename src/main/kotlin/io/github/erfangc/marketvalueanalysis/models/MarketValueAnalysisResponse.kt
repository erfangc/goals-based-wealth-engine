package io.github.erfangc.marketvalueanalysis.models

import io.github.erfangc.assets.models.Asset
import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysis

data class MarketValueAnalysisResponse(
        val marketValueAnalysis: MarketValueAnalysis,
        val assets: Map<String, Asset>
)