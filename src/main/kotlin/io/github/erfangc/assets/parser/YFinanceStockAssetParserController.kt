package io.github.erfangc.assets.parser

import io.github.erfangc.assets.Asset
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/yfinance-fund-asset-parser/_parse")
class YFinanceStockAssetParserController(private val yFinanceStockAssetParser: YFinanceStockAssetParser) {
    @PostMapping
    fun parse(@RequestParam ticker: String): Asset {
        return yFinanceStockAssetParser.parseTicker(ticker)
    }
}