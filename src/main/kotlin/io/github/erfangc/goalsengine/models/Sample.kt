package io.github.erfangc.goalsengine.models

import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysisResponse
import io.github.erfangc.portfolios.models.Portfolio

data class Sample(
        val mu: Double,
        val sigma: Double,
        val portfolio: Portfolio,
        val marketValueAnalysis: MarketValueAnalysisResponse
)