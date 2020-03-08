package io.github.erfangc.proposals

import io.github.erfangc.convexoptimizer.ProposedOrder
import io.github.erfangc.portfolios.Portfolio

data class Proposal(
        val id: String,
        val portfolios: List<Portfolio>,
        val proposedOrders: List<ProposedOrder>,
        val probabilityOfSuccess: Double
)