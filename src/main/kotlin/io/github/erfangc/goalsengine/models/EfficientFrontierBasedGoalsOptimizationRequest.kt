package io.github.erfangc.goalsengine.models

import io.github.erfangc.users.models.WhiteListItem

data class EfficientFrontierBasedGoalsOptimizationRequest(
        val initialWealth: Double,
        val goal: Double,
        val investmentHorizon: Int,
        val cashflows: List<Cashflow>,
        val whiteListItems: List<WhiteListItem>
)