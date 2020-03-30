package io.github.erfangc.goalsengine.internal

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

object BrownianMotionFactory {

    /**
     * Equation 2
     *
     * Creates a function that is essentially a brownian process parametrized (and scaled by an initial wealth)
     * which in turn accepts t and a z (or realization of a normal random)
     */
    fun forWealth(w0: Double, mu: Double, sigma: Double): (Double, Double) -> Double {
        return {
            t: Double, z: Double ->
            w0 * exp((mu - sigma.pow(2.0) / 2) * t + sigma * sqrt(t) * z)
        }
    }

}