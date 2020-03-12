package io.github.erfangc.proposals

import io.github.erfangc.marketvalueanalysis.Allocations

data class GenerateProposalResponseAllocations(
        val original: Allocations,
        val proposed: Allocations
)