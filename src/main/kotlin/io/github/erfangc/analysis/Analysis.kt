package io.github.erfangc.analysis

import io.github.erfangc.marketvalueanalysis.MarketValueAnalysis
import io.github.erfangc.scenarios.ScenarioOutput

data class Analysis(
        val marketValueAnalysis: MarketValueAnalysis,
        val scenarioOutputs: List<ScenarioOutput>,
        val expectedReturn: Double,
        val volatility: Double
)