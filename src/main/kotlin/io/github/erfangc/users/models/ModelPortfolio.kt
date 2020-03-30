package io.github.erfangc.users.models

import io.github.erfangc.portfolios.models.Portfolio
import java.util.*

data class ModelPortfolio(
        val id: String = UUID.randomUUID().toString(),
        val portfolio: Portfolio,
        val labels: List<String> = emptyList()
)