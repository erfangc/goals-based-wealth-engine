package io.github.erfangc.analysis

import io.github.erfangc.clients.Client
import io.github.erfangc.portfolios.Portfolio

data class AnalysisRequest(
        val portfolios: List<Portfolio>,
        val client: Client? = null
)