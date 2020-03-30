package io.github.erfangc.simulatedperformance.models

data class SimulatedPerformanceResponse(
        val timeSeries: List<TimeSeries> = emptyList(),
        val summaryMetrics: SummaryMetrics = SummaryMetrics()
)