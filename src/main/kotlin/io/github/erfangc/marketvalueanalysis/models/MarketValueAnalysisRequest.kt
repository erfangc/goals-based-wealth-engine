package io.github.erfangc.marketvalueanalysis.models

import io.github.erfangc.portfolios.models.Portfolio

data class MarketValueAnalysisRequest(
        val portfolios: List<Portfolio>
)