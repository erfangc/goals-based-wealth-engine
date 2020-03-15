package io.github.erfangc.assets

data class TimeSeriesDatum(
        val assetId: String,
        val date: String,
        val field: Field,
        val value: Double
)