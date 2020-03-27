package io.github.erfangc.goalsengine

import io.github.erfangc.users.settings.ModelPortfolio

data class ModelPortfolioBasedGoalsOptimizationResponse(
        override val expectedReturn: Double,
        override val volatility: Double,
        override val probabilityOfSuccess: Double,
        val modelPortfolio: ModelPortfolio
) : GoalsOptimizationOutput