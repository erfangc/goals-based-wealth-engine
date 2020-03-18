package io.github.erfangc.portfolios.dataimport

data class ResolvePortfolioError(val message: String, val unresolvedIdentifier: String? = null, val index: Int? = null)