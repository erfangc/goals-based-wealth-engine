package io.github.erfangc.goalsengine.models

import io.github.erfangc.users.models.ModelPortfolio

data class ModelPortfolioBasedGoalsOptimizationResponse(
        override val expectedReturn: Double,
        override val volatility: Double,
        override val probabilityOfSuccess: Double,
        val modelPortfolio: ModelPortfolio
) : GoalsOptimizationOutput