package io.github.erfangc.goalsengine

import io.github.erfangc.users.ModelPortfolio

data class GoalsOptimizationRequest(
        val initialWealth: Double,
        val goal: Double,
        val investmentHorizon: Int,
        val cashflows: List<Cashflow>,
        val modelPortfolios: List<ModelPortfolio>? = null
)