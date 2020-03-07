package io.github.erfangc.util

data class MarketValueAnalysis(
        val netAssetValue: Double,
        val marketValue: Map<String, Double>,
        val weights: Map<String, Double>
)