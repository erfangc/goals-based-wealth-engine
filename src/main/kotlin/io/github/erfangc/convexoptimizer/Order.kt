package io.github.erfangc.convexoptimizer

data class Order(
        val assetId: String,
        val portfolioId: String,
        val positionId: String,
        val quantity: Double
)