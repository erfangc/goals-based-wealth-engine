package io.github.erfangc.assets.models

data class TimeSeriesDatum(
        val assetId: String,
        val date: String,
        val field: String,
        val value: Double
)