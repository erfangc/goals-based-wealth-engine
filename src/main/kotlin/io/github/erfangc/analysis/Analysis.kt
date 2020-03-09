package io.github.erfangc.analysis

import io.github.erfangc.marketvalueanalysis.MarketValueAnalysis

data class Analysis(
        val marketValueAnalysis: MarketValueAnalysis,
        val expectedReturn: Double,
        val volatility: Double
)