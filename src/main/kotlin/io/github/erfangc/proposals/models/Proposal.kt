package io.github.erfangc.proposals.models

import io.github.erfangc.convexoptimizer.models.ProposedOrder
import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.users.models.ModelPortfolio

data class Proposal(
        val id: String,
        val clientId: String? = null,
        val portfolios: List<Portfolio>,
        val proposedOrders: List<ProposedOrder>,
        val modelPortfolio: ModelPortfolio? = null
)