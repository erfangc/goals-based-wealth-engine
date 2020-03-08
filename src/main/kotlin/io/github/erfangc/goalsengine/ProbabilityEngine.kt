package io.github.erfangc.goalsengine

import org.apache.commons.math3.distribution.NormalDistribution
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * Unlike the GoalsEngine this class
 * accepts a single expected return and volatility
 * parameter and computes the likelihood of achieving an goal
 *
 * TODO this contains a lot of duplicate logic to GoalsEngine, consolidate them
 */
class ProbabilityEngine(
        private val mu: Double,
        private val sigma: Double,
        private val cashflows: List<Cashflow>,
        private val investmentHorizon: Int,
        private val initialWealth: Double,
        private val goal: Double
) {

    private val log = LoggerFactory.getLogger(GoalsEngine::class.java)
    private val knownCashflowsLookup = cashflows.associateBy { it.t }
    private val c = { t: Int -> knownCashflowsLookup[t]?.amount ?: 0.0 }

    /**
     * Equation 3
     */
    private val wMin = {
        val w = BrownianMotionFactory.forWealth(initialWealth, mu, sigma)
        // select min out of all time
        0.rangeTo(investmentHorizon).map { tau ->
            val cfs = (0..tau).sumByDouble { t ->
                (BrownianMotionFactory.forWealth(c(t), mu, sigma))((tau - t).toDouble(), -3.0)
            }
            w(tau.toDouble(), -3.0) + cfs
        }.min() ?: throw IllegalStateException()
    }()

    /**
     * Equation 4
     */
    private val wMax = {
        val w = BrownianMotionFactory.forWealth(initialWealth, mu, sigma)
        // select min out of all time
        0.rangeTo(investmentHorizon).map { tau ->
            val cfs = (0..tau).sumByDouble { t ->
                (BrownianMotionFactory.forWealth(c(tau), mu, sigma))((tau - t).toDouble(), 3.0)
            }
            w(tau.toDouble(), 3.0) + cfs
        }.max() ?: throw IllegalStateException()
    }()

    /**
     * Represent the state space grid as both a 2-dimensional array as well as a doubly linked list
     * see Section 2.3
     */
    private val nodes: MutableList<MutableList<Node>> = {

        // rhoGrid is the density parameter that controls how sparse and dense the state grid is
        val rhoGrid = 3.0
        val increment = sigma / rhoGrid
        log.info("Creating nodes rhoGrid=${rhoGrid} increment=${sigma / rhoGrid} ln(wMin)=${ln(wMin)} ln(wMax)=${ln(wMax)}")

        // walk up from ln(wMin) -> ln(wMax) by adding 'increment', where ln = natural log
        // then shift the w by an amount necessary so that initial wealth is one of the w
        val wealthStates = mutableListOf(ln(wMin))
        while (wealthStates.last() < ln(wMax)) {
            wealthStates.add(wealthStates.last() + increment)
        }

        // find the smallest shift necessary so that at least 1 node aligns with initial wealth
        val mapIndexed = wealthStates
                .mapIndexed { i, wealth -> i to abs(wealth - ln(initialWealth)) }
        val shift = mapIndexed
                .minBy { it.second }?.let {
                    wealthStates[it.first] - ln(initialWealth)
                } ?: throw IllegalStateException()

        // in shifted wealth states we also undo the natural log transformation we did earlier
        val shiftedWealthStates = wealthStates.map { exp(it - shift) }

        val nodes = 0.rangeTo(investmentHorizon).map { t ->
            shiftedWealthStates.mapIndexed { i, w ->
                // if t = T (i.e. t = investmentHorizon) then we know the answer
                val v = if (t == investmentHorizon) {
                    if (w >= goal) {
                        1.0
                    } else {
                        0.0
                    }
                } else {
                    0.0
                }
                Node(
                        t = t,
                        i = i,
                        w = w,
                        // initialize the mu chosen to 0
                        mu = 0.0,
                        v = v
                )
            }.toMutableList()
        }.toMutableList()
        log.info("Finished creating nodes for the simulation")
        nodes
    }()

    /**
     * Uses the information given to this class to derive an optimal
     * portfolio Node object
     *
     * This object should contain the mu parameter that forms the optimal portfolio (as defined by probability of achieving goals)
     * when chosen from the EfficientFrontier
     */
    fun findOptimalRiskReward(): OptimalRiskReward {
        //
        // define stopping condition:
        // 1) we reached the node such that node.w == W(0)
        // 2) node.t == 0
        //
        // otherwise we select the next node as either the node below in the same time or roll forward to the previous time
        //

        var i = 0

        // start at t = T - 1 (nodes.size - 1 = T)
        var t = nodes.size - 2
        var currentNode: Node
        do {
            currentNode = nodes[t][i]
            // iterate to find the best mu
            val v = v(currentNode, mu)
            nodes[t][i] = currentNode.copy(mu = mu, v = v)
            currentNode = nodes[t][i]
            if (i < nodes[t].size - 1) {
                i++
            } else {
                i = 0
                t--
            }
        } while (!(abs(currentNode.w - initialWealth) < 0.001 && currentNode.t == 0))

        return OptimalRiskReward(
                expectedReturn = currentNode.mu,
                probabilityOfSuccess = currentNode.v,
                volatility = sigma
        )
    }

    private val nd = NormalDistribution()

    /**
     * Equation 6
     *
     * Returns the probability density function of a normal distribution
     * centered at mu, where sigma is given by the efficient frontier
     *
     * This is not the transition probability but an input to it
     */
    private fun pHat(nextNode: Node, currentNode: Node, t: Int): Double {
        val wi = currentNode.w
        val wj = nextNode.w
        val z = (ln(wj / (wi + c(t))) - (mu - sigma.pow(2.0) / 2)) / sigma
        return nd.density(z)
    }

    /**
     * Compute the transition probabilities of a given node
     * to its 'nextNodes'
     */
    private fun v(currentNode: Node, mu: Double): Double {
        // if the nextNodes is null that means we are at the end of the the investment horizon
        // in this case, the "v"s are determined already (they are either 0 or 1 depending on whether the goal have been achieved)

        val nextNodes = nextNodes(currentNode) ?: return currentNode.v

        // memoize the sum of all points on the density function touched by next set of nodes
        // this denominator is used to ensure transition probabilities sum to 1
        val total = nextNodes
                .sumByDouble { nextNode ->
                    pHat(nextNode, currentNode, currentNode.t)
                }

        // sum the next nodes' "v" weighted by probability derived from the prior steps
        return nextNodes.sumByDouble { nextNode ->
            val probability = pHat(nextNode, currentNode, currentNode.t) / total
            nextNode.v * probability
        }
    }

    private fun nextNodes(currentNode: Node): List<Node>? {
        val t = currentNode.t
        return if (t >= investmentHorizon) {
            null
        } else {
            nodes[t + 1]
        }
    }
}