package io.github.erfangc.goalsengine.models

import io.github.erfangc.goalsengine.models.Cashflow

data class TranslateClientGoalsResponse(
        val goal: Double,
        val initialInvestment: Double,
        val cashflows: List<Cashflow>,
        val investmentHorizon: Int
)