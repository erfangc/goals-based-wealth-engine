package io.github.erfangc.proposals.models

import io.github.erfangc.clients.models.Client

data class AnalyzeProposalRequest(val proposal: Proposal, val client: Client? = null)