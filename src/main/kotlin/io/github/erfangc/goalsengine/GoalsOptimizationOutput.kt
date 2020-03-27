package io.github.erfangc.goalsengine

interface GoalsOptimizationOutput {
    val expectedReturn: Double
    val volatility: Double
    val probabilityOfSuccess: Double
}