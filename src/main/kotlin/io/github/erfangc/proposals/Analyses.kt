package io.github.erfangc.proposals

import io.github.erfangc.marketvalueanalysis.MarketValueAnalysis

data class Analyses(
        val marketValueAnalysis: MarketValueAnalysis,
        val expectedReturn: Double,
        val volatility: Double,
        val probabilityOfSuccess: Double
)