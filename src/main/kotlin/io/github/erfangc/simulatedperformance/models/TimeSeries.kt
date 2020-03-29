package io.github.erfangc.simulatedperformance.models

data class TimeSeries(
        val date: String,
        val periodReturn: Double,
        val cumulativeReturn: Double
)