package io.github.erfangc.proposals.models

import io.github.erfangc.clients.models.Client
import io.github.erfangc.convexoptimizer.models.ProposedOrder
import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.users.models.ModelPortfolio

data class Proposal(
        val id: String,
        val name: String,
        val description: String? = null,
        val clientId: String,
        //
        // the client as seen at the time of proposal
        //
        val client: Client? = null,
        //
        // portfolios considered in the proposal at the time
        // of proposal
        //
        val portfolios: List<Portfolio> = emptyList(),
        val proposedOrders: List<ProposedOrder>,
        val modelPortfolio: ModelPortfolio? = null,
        val createdAt: String,
        val updatedAt: String
)