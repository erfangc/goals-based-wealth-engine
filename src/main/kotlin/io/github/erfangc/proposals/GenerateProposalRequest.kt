package io.github.erfangc.proposals

import io.github.erfangc.clients.Client

data class GenerateProposalRequest(
        val client: Client, val newInvestment: Double = 0.0, val save: Boolean = false
)