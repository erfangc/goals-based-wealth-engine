package io.github.erfangc.goalsengine.models

data class EfficientFrontierBasedGoalsOptimizationResponse(
        override val expectedReturn: Double,
        override val volatility: Double,
        override val probabilityOfSuccess: Double
) : GoalsOptimizationOutput