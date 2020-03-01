package io.github.erfangc.goalsbasedwealthengine

import io.github.erfangc.goalsbasedwealthengine.MathUtil.brownianWealthFactory
import org.apache.commons.math3.distribution.NormalDistribution
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * This represents a single C(t) in the vector C
 */
data class KnownCashflow(val t: Int, val amount: Double)

data class Node(
        /**
         * The time coordinate
         */
        val t: Int,
        /**
         * The wealth coordinate
         */
        val i: Int,
        /**
         * The wealth at this node
         */
        val w: Double,
        /**
         * The chosen mu (or expected return on the efficient frontier at this node)
         */
        val mu: Double,
        /**
         * The probability of achieving investment goal at this node
         */
        val v: Double,
        /**
         * Link to the nodes that comes immediately after in time. If null then this is the final set of nodes where t = T
         */
        val nextNodes: List<Node>? = null
)

/**
 * StateSpaceGrid holds information about the state space
 *
 * Each state space represents a potential wealth level at a potential time. Information can then be derived /
 * computed on each node on this grid such as the transition probability between nodes as well
 * as the probability of reaching the goal
 */
class StateSpaceGrid(private val efficientFrontier: EfficientFrontier,
                     val knownCashflows: List<KnownCashflow>,
                     private val investmentHorizon: Int,
                     private val initialWealth: Double,
                     private val goal: Double) {

    private val muMin = efficientFrontier.muMin()
    private val muMax = efficientFrontier.muMax()
    private val sigmaMax = efficientFrontier.sigma(muMax)
    private val sigmaMin = efficientFrontier.sigma(muMin)
    private val threeSigma = 3 * sigmaMax

    /**
     * Equation 3
     */
    private val wMin = {
        val w = brownianWealthFactory(initialWealth, muMin, sigmaMax)
        // select min out of all time
        0.rangeTo(investmentHorizon).map { t ->
            // TODO take into account cash flows
            w(t.toDouble(), -threeSigma)
        }.min() ?: throw IllegalStateException()
    }()

    /**
     * Equation 4
     */
    private val wMax = {
        val w = brownianWealthFactory(initialWealth, muMin, sigmaMax)
        // select min out of all time
        0.rangeTo(investmentHorizon).map { t ->
            w(t.toDouble(), threeSigma)
        }.max() ?: throw IllegalStateException()
    }()

    /**
     * Represent the state space grid as both a 2-dimensional array as well as a doubly linked list
     * see Section 2.3
     */
    private val nodes: MutableList<MutableList<Node>> = {

        // rhoGrid is the density parameter that controls how sparse and dense the state grid is
        val rhoGrid = 15.0
        val increment = sigmaMin / rhoGrid

        // walk up from ln(wMin) -> ln(wMax) by adding 'increment', where ln = natural log
        // then shift the w by an amount necessary so that initial wealth is one of the w
        val wealthStates = mutableListOf(ln(wMin))
        while (wealthStates.last() < ln(wMax)) {
            wealthStates.add(wealthStates.last() + increment)
        }

        // find the smallest shift necessary so that at least 1 node aligns with initial wealth
        val shift = wealthStates
                .mapIndexed { i, wealth -> i to abs(wealth - initialWealth) }
                .minBy { it.second }?.let {
                    wealthStates[it.first] - initialWealth
                } ?: throw IllegalStateException()

        // in shifted wealth states we also undo the natural log transformation we did earlier
        val shiftedWealthStates = wealthStates.map { exp(it + shift) }

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
            }
        }

        // link the nodes
        nodes.map {
            it.map { node ->
                val t = node.t
                val nextNodes = if (t != investmentHorizon) {
                    nodes[t + 1]
                } else {
                    null
                }
                node.copy(nextNodes = nextNodes)
            }.toMutableList()
        }.toMutableList()
    }()

    fun optimize(): Node {
        // starting a t-1, iterate on all mu(s) to find the max one
        val increment = (muMax - muMin) / 15.0
        val musToTry = (0 until 15).map { i -> muMin + i * increment}

        //
        // define stopping condition:
        // 1) we reached the node such that node.w == W(0)
        // 2) node.t == 0
        //
        // otherwise we select the next node as either the node below in the same time or roll forward to the previous time
        //

        var i = 0
        var t = nodes.size - 2 // start at t = T - 1 (nodes.size - 1 = T)
        var currentNode: Node
        do {
            currentNode = nodes[t][i]
            // iterate to find the best mu
            val (mu, v) = musToTry.map { mu ->
                mu to v(currentNode, mu)
            }.minBy { it.second } ?: throw IllegalStateException()
            nodes[t][i] = currentNode.copy(mu = mu, v = v)
            if (i <= nodes[t].size - 1) {
                i++
            } else {
                i = 0
                t--
            }
        } while (!(abs(currentNode.w - initialWealth) < 0.001 && currentNode.t == 0))

        return currentNode
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
    private fun pHat(nextNode: Node, currentNode: Node, mu: Double): Double {
        val sigma = efficientFrontier.sigma(mu)
        val wi = currentNode.w
        val wj = nextNode.w
        val z = ln(wj / wi) - (mu - sigma.pow(2.0) / 2) / sigma
        return nd.density(z)
    }

    /**
     * Compute the transition probabilities of a given node
     * to its 'nextNodes'
     */
    private fun v(currentNode: Node, mu: Double): Double {
        val nextNodes = currentNode.nextNodes ?: return currentNode.v
        // memoize the sum of all points on the density function touched by next set of nodes
        // this denominator is used to ensure transition probabilities sum to 1
        val total = nextNodes
                .sumByDouble { nextNode -> pHat(nextNode, currentNode, mu) }

        // sum the next nodes' "v" weighted by probability derived from the prior steps
        return nextNodes.sumByDouble {
            nextNode ->
            val probability = pHat(nextNode, currentNode, mu) / total
            nextNode.v * probability
        }
    }

}