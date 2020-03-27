package io.github.erfangc.proposals.models

data class GenerateProposalGivenClientIdRequest(
        val newInvestment: Double = 0.0,
        val save: Boolean = false
)