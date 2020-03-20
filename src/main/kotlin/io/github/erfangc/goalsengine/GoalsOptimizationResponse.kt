package io.github.erfangc.goalsengine

import io.github.erfangc.users.ModelPortfolio

data class GoalsOptimizationResponse(
        val expectedReturn: Double,
        val volatility: Double,
        val probabilityOfSuccess: Double,
        val modelPortfolio: ModelPortfolio? = null
)