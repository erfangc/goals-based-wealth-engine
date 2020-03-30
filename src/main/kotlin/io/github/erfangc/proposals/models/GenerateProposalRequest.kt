package io.github.erfangc.proposals.models

import io.github.erfangc.clients.models.Client

data class GenerateProposalRequest(
        val client: Client,
        val newInvestment: Double? = null
)