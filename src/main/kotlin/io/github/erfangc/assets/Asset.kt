package io.github.erfangc.assets

data class Asset(
        val assetId: String,
        val assetClass: String? = null,
        val type: String? = null,
        val name: String? = null,
        val description: String? = null,
        val ticker: String? = null,
        val cusip: String? = null,
        val sedol: String? = null,
        val isin: String? = null,
        val price: Double? = null
)
