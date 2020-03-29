package io.github.erfangc.proposals.models

import io.github.erfangc.clients.Client

data class AnalyzeProposalRequest(val proposal: Proposal, val client: Client? = null)