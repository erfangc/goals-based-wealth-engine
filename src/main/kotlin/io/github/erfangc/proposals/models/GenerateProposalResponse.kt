package io.github.erfangc.proposals.models

import io.github.erfangc.assets.Asset
import io.github.erfangc.portfolios.Portfolio

data class GenerateProposalResponse(
        val proposal: Proposal,
        val analyses: Analyses,
        val proposedPortfolios: List<Portfolio>,
        val assets: Map<String, Asset>
)

