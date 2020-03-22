package io.github.erfangc.users

import io.github.erfangc.portfolios.Portfolio
import java.util.*

data class ModelPortfolio(
        val id: String = UUID.randomUUID().toString(),
        val portfolio: Portfolio
)