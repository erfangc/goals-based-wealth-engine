package io.github.erfangc.proposals.models

import io.github.erfangc.convexoptimizer.ProposedOrder
import io.github.erfangc.portfolios.Portfolio

data class Proposal(
        val id: String,
        val clientId: String? = null,
        val portfolios: List<Portfolio>,
        val proposedOrders: List<ProposedOrder>
)