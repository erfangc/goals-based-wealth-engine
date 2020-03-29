package io.github.erfangc.simulatedperformance.models

data class SimulatedPerformanceResponse(
        val timeSeries: List<TimeSeries>,
        val summaryMetrics: SummaryMetrics
)