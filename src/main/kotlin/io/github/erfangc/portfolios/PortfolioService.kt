package io.github.erfangc.portfolios

import org.springframework.stereotype.Service

@Service
class PortfolioService {
    fun get(id: String): Portfolio {
        TODO()
    }
    fun getForClientId(clientId: String): List<Portfolio>? {
        TODO()
    }
}