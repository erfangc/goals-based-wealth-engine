package io.github.erfangc.portfolios

import org.springframework.stereotype.Service

@Service
class PortfolioService {
    fun getForClientId(clientId: String): List<Portfolio>? {
        return null
    }
    fun savePortfolio(portfolio: Portfolio): Portfolio {
        TODO()
    }
    fun deletePortfolio(id: String): Portfolio? {
        TODO()
    }
    fun getPortfolio(id: String): Portfolio? {
        TODO()
    }
}