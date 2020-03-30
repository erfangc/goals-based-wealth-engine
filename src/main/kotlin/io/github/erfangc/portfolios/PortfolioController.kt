package io.github.erfangc.portfolios

import io.github.erfangc.portfolios.models.Portfolio
import org.springframework.web.bind.annotation.*

@RestController
class PortfolioController(private val portfolioService: PortfolioService) {
    @GetMapping("/apis/portfolios/{id}")
    fun getPortfolio(@PathVariable id: String): Portfolio? {
        return portfolioService.getPortfolio(id)
    }

    @GetMapping("/apis/clients/{clientId}/portfolios")
    fun getPortfoliosForClientId(@PathVariable clientId: String): List<Portfolio>? {
        return portfolioService.getForClientId(clientId)
    }

    @PostMapping("/apis/portfolios")
    fun savePortfolio(@RequestBody portfolio: Portfolio): Portfolio {
        return portfolioService.savePortfolio(portfolio)
    }

    @DeleteMapping("/apis/portfolios/{id}")
    fun deletePortfolio(@PathVariable id: String): Portfolio? {
        return portfolioService.deletePortfolio(id)
    }
}