package io.github.erfangc.clients

data class Goals(
        val retirementYear: Int,
        val retirementYearlyIncome: Double,
        // the year yyyy on which the client is to retire ex: 2040
        val supplementalYearlyIncome: Double = 0.0,
        val knownCashflow: List<KnownCashflow> = emptyList()
)