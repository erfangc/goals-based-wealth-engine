package io.github.erfangc.assets.parser.yfinance

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import io.github.erfangc.assets.*
import io.github.erfangc.assets.parser.ParserUtil.parsePercentage
import io.github.erfangc.assets.parser.ParserUtil.parsePreviousClose
import io.github.erfangc.ddb.DynamoDBUtil.toItem
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import us.codecraft.xsoup.Xsoup

/**
 * This class parses Yahoo finance for
 * summary / holding etc. for mutual funds and ETFs to the extent
 * they disclose that information and is made publicly available
 */
@Service
class YFinanceFundAssetParser(private val ddb: AmazonDynamoDB) {

    private val log = LoggerFactory.getLogger(YFinanceFundAssetParser::class.java)

    fun parseTicker(ticker: String, save: Boolean = false): Asset {
        val allocations = allocations(ticker)

        //
        // find the previous price
        //
        val summary = Jsoup
                .connect("https://finance.yahoo.com/quote/$ticker")
                .get()
        val previousClose = parsePreviousClose(summary)

        //
        // parse fund profile
        //
        val profile = Jsoup
                .connect("https://finance.yahoo.com/quote/$ticker/profile")
                .get()

        val name = Xsoup
                .compile("//*[@id=\"Col1-0-Profile-Proxy\"]/section/div[1]/div[1]/h3")
                .evaluate(profile)
                .elements
                .text()

        val fundOverview = parseTable(Xsoup
                .compile("//*[@id=\"Col1-0-Profile-Proxy\"]/section/div[2]/div[1]")
                .evaluate(profile)
                .elements)

        val fundOperations = parseTable(Xsoup
                .compile("//*[@id=\"Col1-0-Profile-Proxy\"]/section/div[2]/div[2]")
                .evaluate(profile)
                .elements, 1)

        val feesAndExpenses = parseTable(Xsoup
                .compile("//*[@id=\"Col1-0-Profile-Proxy\"]/section/div[2]/div[3]")
                .evaluate(profile)
                .elements, 1)

        val asset = Asset(
                id = ticker,
                ticker = ticker,
                price = previousClose,
                allocations = allocations,
                assetClass = allocations.assetClass.stocks.let { if (it > 0.8) "Stock" else "Bond" },
                name = name,
                `yield` = parsePercentage(fundOverview["Yield"]),
                category = fundOverview["Category"].toString(),
                type = fundOverview["Legal Type"]?.toString() ?: "Mutual Fund"
        )
        if (save) {
            val tableName = "assets"
            log.info("Saving asset $ticker to DynamoDB table $tableName")
            try {
                ddb.putItem(tableName, toItem(asset))
            } catch (e: Exception) {
                log.error("Unable to save ${asset.id} to database", e)
            }
        }
        return asset
    }

    private fun parseTable(elements: Elements, valueColumn: Int? = null): Map<String, Any> {
        if (elements.isEmpty() || elements.first().children().isEmpty()) {
            return emptyMap()
        }
        return elements
                .first()
                .children()[1].children().map { element ->
            val label = element.children().first().text()
            val valueElement = valueColumn
                    ?.let { element.children()[valueColumn] }
                    ?: element.children().last()
            val value = valueElement.text()
            label to value
        }.toMap()
    }

    private fun allocations(ticker: String): Allocations {
        val holdings = Jsoup
                .connect("https://finance.yahoo.com/quote/$ticker/holdings?p=$ticker")
                .get()

        val overallPortfolioCompositionElements = Xsoup
                .compile("//*[@id=\"Col1-0-Holdings-Proxy\"]/section/div[1]/div[1]")
                .evaluate(holdings)
                .elements

        val overallPortfolioCompositions = parseTable(overallPortfolioCompositionElements)

        val assetClassAllocation = AssetClassAllocation(
                stocks = parsePercentage(overallPortfolioCompositions["Stocks"]),
                bonds = parsePercentage(overallPortfolioCompositions["Bonds"])
        )

        //
        // sector weightings
        //
        val sectorWeightsElements = Xsoup
                .compile("//*[@id=\"Col1-0-Holdings-Proxy\"]/section/div[1]/div[2]")
                .evaluate(holdings)
                .elements

        val sectorWeights = parseTable(sectorWeightsElements)

        val gicsAllocation = GicsAllocation(
                basicMaterials = parsePercentage(sectorWeights["Basic Materials"]),
                communicationServices = parsePercentage(sectorWeights["CONSUMER_CYCLICAL"]),
                consumerCyclical = parsePercentage(sectorWeights["Financial Services"]),
                consumerDefensive = parsePercentage(sectorWeights["Realestate"]),
                energy = parsePercentage(sectorWeights["Consumer Defensive"]),
                financialServices = parsePercentage(sectorWeights["Healthcare"]),
                healthCare = parsePercentage(sectorWeights["Utilities"]),
                industrials = parsePercentage(sectorWeights["Communication Services"]),
                realEstate = parsePercentage(sectorWeights["Energy"]),
                technology = parsePercentage(sectorWeights["Industrials"]),
                utilities = parsePercentage(sectorWeights["Technology"])
        )

        //
        // bond ratings
        //
        val bondRatingsElements = Xsoup
                .compile("//*[@id=\"Col1-0-Holdings-Proxy\"]/section/div[2]/div[2]")
                .evaluate(holdings)
                .elements

        val bondRatings = parseTable(bondRatingsElements)

        val bondRatingsAllocation = BondRatingsAllocation(
                usGovernment =  parsePercentage(bondRatings["US Government"]),
                aaa =  parsePercentage(bondRatings["AAA"]),
                aa =  parsePercentage(bondRatings["AA"]),
                a =  parsePercentage(bondRatings["A"]),
                bbb =  parsePercentage(bondRatings["BBB"]),
                bb =  parsePercentage(bondRatings["BB"]),
                b =  parsePercentage(bondRatings["B"]),
                belowB =  parsePercentage(bondRatings["Below B"]),
                others =  parsePercentage(bondRatings["Others"])
        )

        return Allocations(
                assetClass = assetClassAllocation,
                bondRatings = bondRatingsAllocation,
                gicsSectors = gicsAllocation
        )
    }
}