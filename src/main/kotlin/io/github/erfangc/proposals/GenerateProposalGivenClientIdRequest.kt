package io.github.erfangc.proposals

data class GenerateProposalGivenClientIdRequest(
        val newInvestment: Double = 0.0,
        val save: Boolean = false
)