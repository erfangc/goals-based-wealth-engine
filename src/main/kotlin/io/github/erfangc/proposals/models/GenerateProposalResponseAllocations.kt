package io.github.erfangc.proposals.models

import io.github.erfangc.marketvalueanalysis.Allocations

data class GenerateProposalResponseAllocations(
        val original: Allocations,
        val proposed: Allocations
)