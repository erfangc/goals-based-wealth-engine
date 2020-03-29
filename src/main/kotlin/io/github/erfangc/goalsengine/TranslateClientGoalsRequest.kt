package io.github.erfangc.goalsengine

import io.github.erfangc.clients.Client
import io.github.erfangc.portfolios.Portfolio

data class TranslateClientGoalsRequest(
        val client: Client,
        val portfolios: List<Portfolio>
)