package io.github.erfangc.portfolios.models

import java.time.Instant
import java.util.*

data class Portfolio(
        val id: String = UUID.randomUUID().toString(),
        val source: Source? = null,
        val clientId: String? = null,
        val name: String? = null,
        val description: String? = null,
        val positions: List<Position> = emptyList(),
        val lastUpdated: String = Instant.now().toString()
)
