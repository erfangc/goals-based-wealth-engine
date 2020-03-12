package io.github.erfangc.proposals

import io.github.erfangc.assets.Asset

data class GenerateProposalResponse(
        val proposal: Proposal,
        val analyses: Analyses,
        val assets: Map<String, Asset>
)

