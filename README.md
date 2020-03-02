# goals-based-wealth-engine

This is a Kotlin implementation of the dynamic programming approach for deriving optimal portfolio given an efficient frontier

The methodology implements this 2019 [paper](https://srdas.github.io/Papers/DP_Paper.pdf). Please see details here

## How to use it

Go to package `io.github.erfangc.goalsbasedwealthengine`

```kotlin
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

val grid = StateSpaceGrid(
        efficientFrontier = efficientFrontier,
        goal = 200.0,
        initialWealth = 100.0,
        knownCashflows = emptyList(),
        investmentHorizon = 10
)

val node = grid.optimizeAndGetRootNode()
println(node.w)
println(node.v)
println(node.mu)
println(node.t)
```