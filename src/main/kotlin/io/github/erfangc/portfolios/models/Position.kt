package io.github.erfangc.portfolios.models

import java.util.*

data class Position(
        val id: String = UUID.randomUUID().toString(),
        val quantity: Double = 0.0,
        val assetId: String,
        val cost: Double? = null
)