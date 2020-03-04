package io.github.erfangc.assets

data class AssetTimeSeriesDatum(
        val assetId: String,
        val timestamp: String,
        val field: Field,
        val value: Double
)