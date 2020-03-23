package io.github.erfangc.portfolios.dataimport

data class ImportPortfolioRequest(
        val clipboard: String,
        val delimiter: String = ","
)