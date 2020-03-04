package io.github.erfangc.goalsengine

/**
 * This interface defines a set of acceptable portfolio inputs used for goals based planning
 */
interface PortfolioChoices {
    fun mus(): List<Double>
    fun sigma(mu: Double): Double
    fun muMax(): Double
    fun muMin(): Double
}