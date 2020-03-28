package io.github.erfangc.assets.parser.universeproviders

import io.github.erfangc.assets.parser.yfinance.YFinanceStockAssetParser
import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.lang.Exception
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Service
class StockTickerProvider(private val yFinanceStockAssetParser: YFinanceStockAssetParser,
                          private val yFinanceTimeSeriesDownloader: YFinanceTimeSeriesDownloader) {
    private val log = LoggerFactory.getLogger(StockTickerProvider::class.java)
    fun run() {
        // pause 15 seconds per every 100 ticker processed
        val tempCounter = AtomicInteger(0)
        val totalCount = AtomicInteger(0)
        listOf("NASDAQ.csv", "NYSE.csv")
                .forEach { record ->
                    val inputStream = ClassPathResource(record).inputStream
                    val csvParser = CSVParser(inputStream.bufferedReader(), CSVFormat.DEFAULT.withFirstRecordAsHeader())
                    csvParser.forEach {
                        val ticker = it.get("Symbol")
                        try {
                            val sector = it.get("Sector")
                            log.info("Processing $ticker in sector $sector")
                            if (tempCounter.get() >= 100) {
                                log.info("Sleeping 10 seconds to avoid throttling")
                                Thread.sleep(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS))
                                tempCounter.set(0)
                            }
                            if (sector != "n/a") {
                                yFinanceStockAssetParser.parseTicker(ticker, true)
                                yFinanceTimeSeriesDownloader.downloadHistoryForTicker(ticker, save  = true)
                                val totalProcessed = totalCount.incrementAndGet()
                                log.info("Finished processing $ticker, totalProcessed=$totalProcessed")
                            }
                        } catch (e: Exception) {
                            log.error("Unable to process ticker $ticker", e)
                        }
                        tempCounter.incrementAndGet()
                    }
                    csvParser.close()
                }
    }
}