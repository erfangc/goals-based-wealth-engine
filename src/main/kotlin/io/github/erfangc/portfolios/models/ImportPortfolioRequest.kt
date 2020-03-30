package io.github.erfangc.portfolios.models

data class ImportPortfolioRequest(
        val clipboard: String,
        val delimiter: String = ","
)