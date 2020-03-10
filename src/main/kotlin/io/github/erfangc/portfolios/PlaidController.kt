package io.github.erfangc.portfolios

import org.springframework.web.bind.annotation.*

@RestController
class PlaidController(private val plaidService: PlaidService) {
    @PostMapping("/apis/clients/{clientId}/plaid/_link")
    fun linkItem(@PathVariable clientId: String, @RequestParam publicToken: String): List<Portfolio> {
        return plaidService.linkItem(clientId, publicToken)
    }
}