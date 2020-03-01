package io.github.erfangc.goalsbasedwealthengine

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