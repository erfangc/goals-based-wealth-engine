package io.github.erfangc.marketvalueanalysis.models

import io.github.erfangc.marketvalueanalysis.models.Allocations

data class MarketValueAnalysis(
        val netAssetValue: Double,
        val netAssetValues: Map<String, Double>,
        val marketValue: Map<String, Map<String, Double>>,
        val weights: Map<String, Map<String, Double>>,
        val weightsToAllInvestments: Map<String, Map<String, Double>>,
        val allocations: Allocations
)
