package io.github.erfangc.goalsbasedwealthengine

import org.junit.jupiter.api.Test

internal class EfficientFrontierTest {

    @Test
    fun sigma() {
        val efficientFrontier = EfficientFrontier(
                covarianceMatrix = arrayOf(
                        doubleArrayOf(0.0017, -0.0017, -0.0021),
                        doubleArrayOf(-0.0017, -0.0396, 0.03086),
                        doubleArrayOf(-0.0021, 0.03086, 0.0392)
                ),
                expectedReturns = doubleArrayOf(
                        0.0493,
                        0.0770,
                        0.0886
                )
        )
        println(efficientFrontier.sigma(0.05))
        println(efficientFrontier.sigma(0.06))
        println(efficientFrontier.sigma(0.07))
        println(efficientFrontier.sigma(0.08))
        println(efficientFrontier.sigma(0.09))
    }

}