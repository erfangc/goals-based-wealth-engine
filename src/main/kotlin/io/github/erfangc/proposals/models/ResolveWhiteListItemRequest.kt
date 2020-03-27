package io.github.erfangc.proposals.models

import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.users.settings.ModelPortfolio

data class ResolveWhiteListItemRequest(
        val portfolio: Portfolio,
        val modelPortfolio: ModelPortfolio? = null
)