package io.github.erfangc.clients

import java.time.LocalDate

data class Goals(
        val retirement: LocalDate,
        val retirementYearlyIncome: Double,
        val supplementalYearlyIncome: Double = 0.0,
        val knownCashflows: List<KnownCashflow> = emptyList()
)