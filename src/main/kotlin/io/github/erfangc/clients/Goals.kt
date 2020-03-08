package io.github.erfangc.clients

import java.time.LocalDate

data class Goals(
        val retirement: LocalDate,
        val retirementYearlyIncome: Double,
        // the year yyyy on which the client is to retire ex: 2040
        val supplementalYearlyIncome: Double = 0.0,
        val knownCashflows: List<KnownCashflow> = emptyList()
)