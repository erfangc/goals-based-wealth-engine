package io.github.erfangc.goalsengine

import io.github.erfangc.goalsengine.portfoliochoices.Cashflow

data class TranslateClientGoalsResponse(
        val goal: Double,
        val initialInvestment: Double,
        val cashflows: List<Cashflow>,
        val investmentHorizon: Int
)