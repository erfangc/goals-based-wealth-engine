package io.github.erfangc.analysis

import io.github.erfangc.marketvalueanalysis.MarketValueAnalysis
import io.github.erfangc.scenarios.ScenarioOutput
import io.github.erfangc.simulatedperformance.models.SummaryMetrics
import io.github.erfangc.simulatedperformance.models.TimeSeries

data class Analysis(
        val marketValueAnalysis: MarketValueAnalysis,
        val scenarioOutputs: List<ScenarioOutput>,
        val simulatedPerformance: List<TimeSeries>,
        val simulatedPerformanceSummaryMetrics: SummaryMetrics,
        val probabilityOfSuccess: Double? = null,
        val expectedReturn: Double,
        val volatility: Double
)