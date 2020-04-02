package io.github.erfangc.proposals.models

import io.github.erfangc.convexoptimizer.models.ProposedOrder
import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.users.models.ModelPortfolio

data class Proposal(
        val id: String,
        val name: String,
        val description: String? = null,
        val clientId: String,
        val portfolios: List<Portfolio>,
        val proposedOrders: List<ProposedOrder>,
        val modelPortfolio: ModelPortfolio? = null,
        val createdAt: String,
        val updatedAt: String
)