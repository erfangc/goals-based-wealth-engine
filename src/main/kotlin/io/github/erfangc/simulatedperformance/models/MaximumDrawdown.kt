package io.github.erfangc.simulatedperformance.models

import java.time.LocalDate

data class MaximumDrawdown(
        val value: Double = 0.0,
        val start: String = LocalDate.MIN.toString(),
        val stop: String = LocalDate.MAX.toString()
)