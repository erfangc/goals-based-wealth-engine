package io.github.erfangc.portfolios

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/plaid")
class PlaidController(private val plaidService: PlaidService) {
    @PostMapping("_link")
    fun linkItem(@PathVariable clientId: String, @RequestParam publicToken: String): List<Portfolio> {
        return plaidService.linkItem(clientId, publicToken)
    }
}