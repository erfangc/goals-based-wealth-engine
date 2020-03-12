package io.github.erfangc.marketvalueanalysis

import io.github.erfangc.assets.Asset

data class MarketValueAnalysisResponse(
        val marketValueAnalysis: MarketValueAnalysis,
        val assets: Map<String, Asset>
)