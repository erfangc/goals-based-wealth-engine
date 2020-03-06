package io.github.erfangc.portfolios

data class Portfolio(
        val id: String,
        val name: String? = null,
        val description: String? = null,
        val positions: List<Position> = emptyList()
)
