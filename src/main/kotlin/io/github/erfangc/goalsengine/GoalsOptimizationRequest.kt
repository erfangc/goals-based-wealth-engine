package io.github.erfangc.goalsengine

data class GoalsOptimizationRequest(
        val initialWealth: Double,
        val goal: Double,
        val investmentHorizon: Int,
        val cashflows: List<Cashflow>
)