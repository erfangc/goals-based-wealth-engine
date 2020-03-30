package io.github.erfangc.goalsengine.models

import io.github.erfangc.clients.models.Client
import io.github.erfangc.portfolios.models.Portfolio

data class TranslateClientGoalsRequest(
        val client: Client,
        val portfolios: List<Portfolio>
)