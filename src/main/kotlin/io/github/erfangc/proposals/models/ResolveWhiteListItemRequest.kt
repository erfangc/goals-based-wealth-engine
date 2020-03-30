package io.github.erfangc.proposals.models

import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.users.models.ModelPortfolio

data class ResolveWhiteListItemRequest(
        val portfolio: Portfolio,
        val modelPortfolio: ModelPortfolio? = null
)