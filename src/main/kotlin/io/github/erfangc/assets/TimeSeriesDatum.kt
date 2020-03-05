package io.github.erfangc.assets

import java.time.LocalDate

data class TimeSeriesDatum(
        val assetId: String,
        val date: LocalDate,
        val field: Field,
        val value: Double
)