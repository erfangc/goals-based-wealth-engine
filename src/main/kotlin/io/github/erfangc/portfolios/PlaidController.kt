package io.github.erfangc.portfolios

import io.github.erfangc.portfolios.models.LinkItemResponse
import org.springframework.web.bind.annotation.*

@RestController
class PlaidController(private val plaidService: PlaidService) {
    @PostMapping("/apis/clients/{clientId}/plaid/_link")
    fun linkItem(@PathVariable clientId: String, @RequestParam publicToken: String): LinkItemResponse {
        return plaidService.linkItem(clientId, publicToken)
    }
}