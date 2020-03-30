package io.github.erfangc.proposals.models

import io.github.erfangc.convexoptimizer.models.ProposedOrder
import io.github.erfangc.portfolios.models.Portfolio

data class Proposal(
        val id: String,
        val clientId: String? = null,
        val portfolios: List<Portfolio>,
        val proposedOrders: List<ProposedOrder>
)