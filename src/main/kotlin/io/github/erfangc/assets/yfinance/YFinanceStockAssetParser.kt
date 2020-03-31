package io.github.erfangc.assets.yfinance

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.CaseFormat
import io.github.erfangc.assets.models.Allocations
import io.github.erfangc.assets.models.Asset
import io.github.erfangc.assets.models.AssetClassAllocation
import io.github.erfangc.assets.models.GicsAllocation
import io.github.erfangc.assets.internal.ParserUtil.parsePreviousClose
import io.github.erfangc.common.DynamoDBUtil
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import us.codecraft.xsoup.Xsoup

/**
 * This class parses Yahoo finance for
 * summary / holding etc. for mutual funds and ETFs to the extent
 * they disclose that information and is made publicly available
 */
@Service
class YFinanceStockAssetParser(private val objectMapper: ObjectMapper, private val ddb: AmazonDynamoDB) {

    private val log = LoggerFactory.getLogger(YFinanceFundAssetParser::class.java)

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
                .text()?.let { if (it.isBlank()) "Unknown" else it } ?: "Unknown"

        val previousClose = parsePreviousClose(summary)

        val gicsSector = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, sector.replace(" ", ""))

        val gicsSectors = determineGicsSector(gicsSector)

        val asset = Asset(
                id = ticker,
                ticker = ticker,
                assetClass = "Stock",
                type = "Stock",
                name = name,
                price = previousClose,
                allocations = Allocations(
                        assetClass = AssetClassAllocation(stocks = 100.0),
                        gicsSectors = gicsSectors
                )
        )
        if (save) {
            val tableName = "assets"
            log.info("Saving asset $ticker to DynamoDB table $tableName")
            try {
                ddb.putItem(tableName, DynamoDBUtil.toItem(asset))
            } catch (e: Exception) {
                log.error("Unable to save ${asset.id} to database", e)
            }
        }
        return asset
    }

    fun determineGicsSector(sector: String): GicsAllocation {
        return when (sector) {
            "Basic Materials" -> {
                GicsAllocation(basicMaterials = 100.0)
            }
            "Consumer Cyclical" -> {
                GicsAllocation(consumerCyclical = 100.0)
            }
            "Financial Services" -> {
                GicsAllocation(financialServices = 100.0)
            }
            "Real Estate" -> {
                GicsAllocation(realEstate = 100.0)
            }
            "Consumer Defense" -> {
                GicsAllocation(consumerDefensive = 100.0)
            }
            "Healthcare" -> {
                GicsAllocation(healthCare = 100.0)
            }
            "Utilities" -> {
                GicsAllocation(utilities = 100.0)
            }
            "Communication Services" -> {
                GicsAllocation(communicationServices = 100.0)
            }
            "Energy" -> {
                GicsAllocation(energy = 100.0)
            }
            "Industrials" -> {
                GicsAllocation(industrials = 100.0)
            }
            "Technology" -> {
                GicsAllocation(technology = 100.0)
            }
            else -> {
                GicsAllocation()
            }
        }
    }
}
