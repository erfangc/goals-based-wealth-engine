package io.github.erfangc.analysis.models

import io.github.erfangc.clients.models.Client
import io.github.erfangc.portfolios.models.Portfolio

data class AnalysisRequest(
        val portfolios: List<Portfolio>,
        val client: Client? = null
)