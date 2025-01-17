package io.github.erfangc.proposals.models

import io.github.erfangc.assets.models.Asset
import io.github.erfangc.portfolios.models.Portfolio

data class AnalyzeProposalResponse(
        val analyses: Analyses,
        val proposedPortfolios: List<Portfolio>,
        val assets: Map<String, Asset>
)