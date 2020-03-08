package io.github.erfangc.marketvalueanalysis

data class MarketValueAnalysis(
        val netAssetValue: Double,
        val marketValue: Map<String, Double>,
        val weights: Map<String, Double>
)