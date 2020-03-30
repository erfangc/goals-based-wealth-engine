package io.github.erfangc.goalsengine

import io.github.erfangc.goalsengine.internal.GoalsEngine
import io.github.erfangc.goalsengine.models.EfficientFrontier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GoalsEngineTest {

    @Test
    internal fun name() {
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
        val grid = GoalsEngine(
                portfolioChoices = efficientFrontier,
                goal = 200.0,
                initialWealth = 100.0,
                cashflows = emptyList(),
                investmentHorizon = 10
        )
        val optimalRiskReward = grid.findOptimalRiskReward()
        assertEquals(0.6645170251146737, optimalRiskReward.probabilityOfSuccess)
        assertEquals(0.0814, optimalRiskReward.expectedReturn)
        assertEquals(0.16038768057568525, optimalRiskReward.volatility)
    }

}