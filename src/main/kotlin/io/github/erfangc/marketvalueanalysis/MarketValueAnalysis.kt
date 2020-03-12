package io.github.erfangc.marketvalueanalysis

data class MarketValueAnalysis(
        val netAssetValue: Double,
        val netAssetValues: Map<String, Double>,
        val marketValue: Map<String, Map<String, Double>>,
        val weights: Map<String, Map<String, Double>>,
        val allocations: Allocations
)
