package io.github.erfangc.marketvalueanalysis

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/market-value-analyses")
class MarketValueAnalysisController(private val marketValueAnalysisService: MarketValueAnalysisService) {
    @PostMapping
    fun marketValueAnalysis(@RequestBody req: MarketValueAnalysisRequest): MarketValueAnalysisResponse {
        return marketValueAnalysisService.marketValueAnalysis(req)
    }
}