package io.github.erfangc.portfolios

import java.time.Instant

data class Portfolio(
        val id: String,
        val source: Source? = null,
        val clientId: String? = null,
        val name: String? = null,
        val description: String? = null,
        val positions: List<Position> = emptyList(),
        val lastUpdated: String = Instant.now().toString()
)
