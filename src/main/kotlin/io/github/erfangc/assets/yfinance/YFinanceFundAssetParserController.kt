package io.github.erfangc.assets.yfinance

import io.github.erfangc.assets.models.Asset
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/yfinance-fund-asset-parser/_parse")
class YFinanceFundAssetParserController(private val yFinanceFundAssetParser: YFinanceFundAssetParser) {
    @PostMapping
    fun parse(@RequestParam ticker: String, @RequestParam(required = false) save: Boolean = false): Asset {
        return yFinanceFundAssetParser.parseTicker(ticker, save)
    }
}
