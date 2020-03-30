package io.github.erfangc.clients.models

import java.time.LocalDate

data class Goals(
        val retirement: LocalDate,
        val retirementYearlyIncome: Double,
        val supplementalYearlyIncome: Double = 0.0,
        val knownCashflows: List<KnownCashflow> = emptyList(),
        /**
         * this can either be 'efficient frontier' or 'model portfolio'
         */
        val approach: String = "efficient frontier",
        val autoAssignModelPortfolio: Boolean? = null
)