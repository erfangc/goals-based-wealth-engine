package io.github.erfangc.assets.parser.universeproviders

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.erfangc.assets.parser.yfinance.YFinanceFundAssetParser
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesParser
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Service
class MutualFundTickersProvider(private val httpClient: HttpClient,
                                private val objectMapper: ObjectMapper,
                                private val yFinanceFundAssetParser: YFinanceFundAssetParser,
                                private val yFinanceTimeSeriesParser: YFinanceTimeSeriesParser
) {

    private val log = LoggerFactory.getLogger(MutualFundTickersProvider::class.java)

    /**
     * Queries mutualfund.com's API to find mutual funds that we can import into our
     * universe. For each imported ticker, we download both the time series of adjusted close
     * from Yahoo finance as well as other indicative data
     */
    fun run() {
        val page = AtomicInteger(1)
        val totalPages = AtomicInteger(1)
        do {
            // this operation mutates totalPages
            try {
                processPage(page, totalPages)
            } catch (e: Exception) {
                log.error("Unable to process page ${page.get()}, skipping")
            }
            Thread.sleep(TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS))
            page.incrementAndGet()
        } while (page.get() <= totalPages.get())


    }

    private fun processPage(page: AtomicInteger, totalPages: AtomicInteger) {
        val post = HttpPost("https://mutualfunds.com/api/data_set/")
                .apply {
                    addHeader("content-type", "application/json")
                    addHeader("origin", "https://mutualfunds.com")
                    entity = StringEntity("""
                            {"tm":"1-fund-category","r":"Channel#531","only":["meta","data"],"page":${page.get()},"default_tab":"overview"}
                        """.trimIndent())
                }
        val content = httpClient
                .execute(post)
                .entity
                .content

        val jsonNode = objectMapper.readTree(content)
        totalPages.set(jsonNode.at("/meta/total_pages").asInt())

        // process each data item
        val data = jsonNode.at("/data")
        data.forEachIndexed { idx, datum ->
            val primaryCategory = datum.at("/primary-category/text").textValue()
            val shareClassSymbol = datum.at("/share_class_symbol").textValue()
            val ticker = "\\w+\\s\\((\\w+)\\)".toRegex().matchEntire(shareClassSymbol)?.groupValues?.last()
            if (primaryCategory != "Money Market") {
                processSingleTicker(ticker, page, idx)
            } else {
                log.info("Skipping processing of money market fund $ticker")
            }
        }
        log.info("Finished processing page ${page.get()}")
    }

    private fun processSingleTicker(ticker: String?, page: AtomicInteger, idx: Int) {
        if (ticker != null) {
            log.info("Processing ticker $ticker for page ${page.get()} element $idx")
            try {
                yFinanceFundAssetParser.parseTicker(ticker, true)
                yFinanceTimeSeriesParser.downloadHistoryForTicker(ticker, true)
            } catch (e: Exception) {
                log.error("Unable to fully process ticker $ticker", e)
            }
        } else {
            log.error("Unable to extract ticker for page ${page.get()} element $idx")
        }
    }
}