# goals-based-wealth-engine

This is a Kotlin implementation of the dynamic programming approach for deriving optimal portfolio given an efficient frontier

The methodology implements this 2019 [paper](https://srdas.github.io/Papers/DP_Paper.pdf). Please see details here

## How to use it

Go to package `io.github.erfangc.goalsbasedwealthengine`

Create an instance of `StateSpaceGrid` and passing it an instance of `EfficientFrontier`

### Example (from unit test):

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

Calling `optimizeAndGetRootNode()`
results in the dynamic programming problem being solved and the solution `Node` being the output

This `Node` represents an optimal `μ` for which there is an corresponding `σ` on the efficient frontier
obtained via `EfficientFrontier##sigma(μ)`

With the target `μ` and `σ`, we can then back out the target portfolio

## What is `StateSpaceGrid`?

The paper above explains everything in much more detail. Here I will give the abridged version:

 - We represent wealth as a 2D grid. On one dimension is time, on the other is a set of possible
 wealth levels at the given time. _This is the state space_
 
 - Each node on this state space grid have some probability of transitioning to another node at time T+1
 