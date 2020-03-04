package io.github.erfangc.assets

data class Asset(
        val assetId: String,
        val name: String? = null,
        val description: String? = null,
        val ticker: String? = null,
        val cusip: String? = null,
        val sedol: String? = null,
        val isin: String? = null,
        val type: String? = null,
        val price: Double? = null,
        val expectedReturn: Double? = null
)

data class AssetTimeSeriesDatum(
        val assetId: String,
        val timestamp: String,
        val field: Field,
        val value: Double
)

enum class Field {
    PRICE, RETURN
}