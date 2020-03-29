package io.github.erfangc.proposals.models

import io.github.erfangc.assets.Asset
import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.proposals.models.Analyses

data class AnalyzeProposalResponse(
        val analyses: Analyses,
        val proposedPortfolios: List<Portfolio>,
        val assets: Map<String, Asset>
)