package io.github.erfangc.proposals.models

import io.github.erfangc.clients.Client

data class GenerateProposalRequest(
        val client: Client,
        val newInvestment: Double? = null
)