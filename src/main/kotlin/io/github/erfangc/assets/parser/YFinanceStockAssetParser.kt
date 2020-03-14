package io.github.erfangc.assets.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.CaseFormat
import io.github.erfangc.assets.*
import io.github.erfangc.assets.parser.ParserUtil.parsePreviousClose
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import us.codecraft.xsoup.Xsoup

/**
 * This class parses Yahoo finance for
 * summary / holding etc. for mutual funds and ETFs to the extent
 * they disclose that information and is made publicly available
 */
@Service
class YFinanceStockAssetParser(private val objectMapper: ObjectMapper) {

    fun parseTicker(ticker: String, save: Boolean = false): Asset {
        val profile = Jsoup
                .connect("https://finance.yahoo.com/quote/$ticker/profile")
                .get()
        val sector = profile
                .select(
                "#Col1-0-Profile-Proxy > section > div.asset-profile-container > div > div > p:nth-child(2) > span:nth-child(2)"
                )
                .text()

        val summary = Jsoup
                .connect("https://finance.yahoo.com/quote/$ticker")
                .get()

        val name = Xsoup
                .compile("//*[@id=\"Col1-0-Profile-Proxy\"]/section/div[1]/div[1]/h3")
                .evaluate(profile)
                .elements
                .text()

        val previousClose = parsePreviousClose(summary)

        val gicsSector = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, sector.replace(" ", ""))

        return Asset(
                id = ticker,
                ticker = ticker,
                name = name,
                price = previousClose,
                allocations = Allocations(
                        assetClass = AssetClassAllocation(stocks = 100.0),
                        gicsSectors = objectMapper.readValue("{\"$gicsSector\": 100.0}")
                )
        )
    }

}