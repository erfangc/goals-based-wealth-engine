package io.github.erfangc.marketvalueanalysis

import io.github.erfangc.portfolios.Portfolio

data class MarketValueAnalysisRequest(
        val portfolios: List<Portfolio>
)