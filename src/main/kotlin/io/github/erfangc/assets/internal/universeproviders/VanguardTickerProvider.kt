package io.github.erfangc.assets.internal.universeproviders

import io.github.erfangc.assets.yfinance.YFinanceFundAssetParser
import io.github.erfangc.assets.yfinance.YFinanceTimeSeriesDownloader
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import kotlin.streams.toList

@Service
class VanguardTickerProvider(
        private val yFinanceFundAssetParser: YFinanceFundAssetParser,
        private val yFinanceTimeSeriesDownloader: YFinanceTimeSeriesDownloader
) {
    private val log = LoggerFactory.getLogger(VanguardTickerProvider::class.java)

    @Scheduled(cron = "0 22 * * 5 ?")
    fun run() {
        ClassPathResource("vanguard-etfs.csv")
                .inputStream
                .bufferedReader()
                .lines()
                .toList()
                .forEachIndexed {
                    idx, row ->
                    try {
                        val ticker = row.trim()
                        log.info("Processing ticker $ticker")
                        yFinanceFundAssetParser.parseTicker(ticker, true)
                        yFinanceTimeSeriesDownloader.downloadHistoryForTicker(ticker, save = true)
                        log.info("Finished processing ticker $ticker")
                    } catch (e: Exception) {
                        log.error("Unable to process row $idx", e)
                    }
                }
    }

}