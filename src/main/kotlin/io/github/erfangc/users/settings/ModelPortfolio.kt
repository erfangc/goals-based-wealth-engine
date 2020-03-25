package io.github.erfangc.users.settings

import io.github.erfangc.portfolios.Portfolio
import java.util.*

data class ModelPortfolio(
        val id: String = UUID.randomUUID().toString(),
        val portfolio: Portfolio,
        val labels: List<String> = emptyList()
)