package io.github.erfangc.assets.yfinance

import io.github.erfangc.assets.models.TimeSeriesDatum
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/yfinance-time-series-downloader/_download")
class YFinanceTimeSeriesDownloaderController(private val yFinanceTimeSeriesDownloader: YFinanceTimeSeriesDownloader) {
    @PostMapping
    fun downloadHistoryForTicker(
            @RequestParam ticker: String,
            @RequestParam(required = false) save: Boolean = false): List<TimeSeriesDatum> {
        return yFinanceTimeSeriesDownloader.downloadHistoryForTicker(ticker, save = save)
    }
}