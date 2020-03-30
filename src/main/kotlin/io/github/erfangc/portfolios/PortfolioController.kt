package io.github.erfangc.portfolios

import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.portfolios.models.SavePortfolioRequest
import org.springframework.web.bind.annotation.*

@RestController
class PortfolioController(private val portfolioService: PortfolioService) {
    @GetMapping("/apis/clients/{clientId}/portfolios/{id}")
    fun getPortfolio(@PathVariable clientId: String, @PathVariable id: String): Portfolio {
        return portfolioService.getPortfolio(clientId, id).portfolio
    }

    @GetMapping("/apis/clients/{clientId}/portfolios")
    fun getPortfoliosForClientId(@PathVariable clientId: String): List<Portfolio> {
        return portfolioService.getPortfoliosForClient(clientId).portfolios
    }

    @PostMapping("/apis/clients/{clientId}/portfolios")
    fun savePortfolio(@PathVariable clientId: String, @RequestBody portfolio: Portfolio): Portfolio {
        return portfolioService.savePortfolio(clientId, SavePortfolioRequest(portfolio)).portfolio
    }

    @DeleteMapping("/apis/clients/{clientId}/portfolios/{id}")
    fun deletePortfolio(@PathVariable clientId: String, @PathVariable id: String): Portfolio {
        return portfolioService.deletePortfolio(clientId, id).portfolio
    }
}