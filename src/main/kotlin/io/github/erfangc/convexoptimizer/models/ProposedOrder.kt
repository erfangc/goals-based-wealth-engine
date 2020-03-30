package io.github.erfangc.convexoptimizer.models

data class ProposedOrder(
        val assetId: String,
        val portfolioId: String,
        val positionId: String,
        val quantity: Double,
        val description: String = ""
)