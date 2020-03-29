package io.github.erfangc.goalsengine.models

interface GoalsOptimizationOutput {
    val expectedReturn: Double
    val volatility: Double
    val probabilityOfSuccess: Double
}