package io.github.erfangc.scenarios

data class TimeSeriesDefinition(
        val id: String,
        val name: String,
        val assetId: String,
        val url: String? = null,
        val description: String? = null
)