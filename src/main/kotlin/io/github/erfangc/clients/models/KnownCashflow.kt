package io.github.erfangc.clients.models

data class KnownCashflow(
        val year: Int,
        val amount: Double,
        val name: String? = null
)