package io.github.erfangc.goalsengine

import io.github.erfangc.users.settings.WhiteListItem

data class EfficientFrontierBasedGoalsOptimizationRequest(
        val initialWealth: Double,
        val goal: Double,
        val investmentHorizon: Int,
        val cashflows: List<Cashflow>,
        val whiteListItems: List<WhiteListItem>
)