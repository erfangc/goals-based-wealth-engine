package io.github.erfangc.proposals

import io.github.erfangc.analysis.Analysis
import io.github.erfangc.convexoptimizer.ProposedOrder
import io.github.erfangc.portfolios.Portfolio

data class Proposal(
        val id: String,
        val clientId: String? = null,
        val portfolios: List<Portfolio>,
        val proposedOrders: List<ProposedOrder>,
        val probabilityOfSuccess: Double,
        val analysis: Analysis
)