package io.github.erfangc.goalsengine.models

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.MatrixUtils
import kotlin.math.pow


/**
 * This class implements Equation 1
 *
 * This is a class that represents an efficient frontier that allows conversion from
 * arbitrary return levels to a corresponding vol level on the frontier
 *
 * This class accepts vector of expected returns & a square covariance matrix
 */
class EfficientFrontier(covarianceMatrix: Array<DoubleArray>, expectedReturns: DoubleArray) : PortfolioChoices {

    init {
        if (expectedReturns.size != covarianceMatrix.size) {
            throw IllegalStateException("expectedReturns do not have the same size as covarianceMatrix")
        }
    }

    private val m = Array2DRowRealMatrix(expectedReturns)
    private val sigma = Array2DRowRealMatrix(covarianceMatrix)
    private val ones = Array2DRowRealMatrix(expectedReturns.map { 1.0 }.toDoubleArray())

    private val sigmaInverse = MatrixUtils.inverse(sigma)
    private val k = m.transpose().multiply(sigmaInverse).multiply(ones).getEntry(0, 0)
    private val l = m.transpose().multiply(sigmaInverse).multiply(m).getEntry(0, 0)
    private val p = ones.transpose().multiply(sigmaInverse).multiply(ones).getEntry(0, 0)

    private val divScalar = l * p - k.pow(2.0)

    private val g = sigmaInverse
            .multiply(ones)
            .scalarMultiply(l)
            .subtract(
                    sigmaInverse
                            .multiply(m)
                            .scalarMultiply(k)
            )
            .scalarMultiply(1.0 / divScalar)

    private val h = sigmaInverse
            .multiply(m)
            .scalarMultiply(p)
            .subtract(
                    sigmaInverse
                            .multiply(ones)
                            .scalarMultiply(k)
            )
            .scalarMultiply(1.0 / divScalar)

    private val a = h.transpose().multiply(sigma).multiply(h).getEntry(0, 0)
    private val b = g.scalarMultiply(2.0).transpose().multiply(sigma).multiply(h).getEntry(0, 0)
    private val c = g.transpose().multiply(sigma).multiply(g).getEntry(0, 0)

    override fun mus(): List<Double> {
        // starting a t-1, iterate on all mu(s) to find the max one
        val increment = (muMax() - muMin()) / 15.0
        return (0 until 15).map { i -> muMin() + i * increment}
    }

    override fun sigma(mu: Double): Double {
        return (a * mu.pow(2.0) + b * mu + c).pow(0.5)
    }

    override fun muMin(): Double {
        return 0.0526
    }

    override fun muMax(): Double {
        return 0.0886
    }

}