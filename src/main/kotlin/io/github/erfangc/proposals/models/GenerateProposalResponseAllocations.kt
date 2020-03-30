package io.github.erfangc.proposals.models

import io.github.erfangc.marketvalueanalysis.models.Allocations

data class GenerateProposalResponseAllocations(
        val original: Allocations,
        val proposed: Allocations
)