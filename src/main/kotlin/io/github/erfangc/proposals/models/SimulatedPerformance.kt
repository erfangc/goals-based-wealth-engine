package io.github.erfangc.proposals.models

import io.github.erfangc.simulatedperformance.models.SummaryMetrics
import io.github.erfangc.simulatedperformance.models.TimeSeries

data class SimulatedPerformance(
        val timeSeries: List<TimeSeries>,
        val summaryMetrics: SummaryMetrics
)