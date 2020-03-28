package io.github.erfangc.scenarios

import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.springframework.stereotype.Service

@Service
class TimeSeriesDefinitionService(private val yFinanceTimeSeriesDownloader: YFinanceTimeSeriesDownloader) {

    private val timeSeriesDefinitions = listOf(
            TimeSeriesDefinition(id = "^GSPC", name = "S&P 500", assetId = "^GSPC", description = "S&P 500"),
            TimeSeriesDefinition(id = "^N225", name = "Nikkei 225", assetId = "^N225", description = "Nikkei 225"),
            TimeSeriesDefinition(id = "^HSI", name = "Hang Seng Index", assetId = "^HSI", description = "Hang Seng Index"),
            TimeSeriesDefinition(id = "^VIX", name = "VIX", assetId = "^VIX", description = "VIX"),
            TimeSeriesDefinition(id = "^FTSE", name = "FTSE", assetId = "^FTSE", description = "FTSE"),
            TimeSeriesDefinition(id = "GC=F", name = "Gold", assetId = "GC=F", description = "Gold"),
            TimeSeriesDefinition(id = "SI=F", name = "Silver", assetId = "SI=F", description = "Silver"),
            TimeSeriesDefinition(id = "CL=F", name = "Crude Oil", assetId = "CL=F", description = "Crude Oil"),
            TimeSeriesDefinition(id = "NG=F", name = "Natural Gas", assetId = "NG=F", description = "Natural Gas"),
            TimeSeriesDefinition(id = "ZN=F", name = "10 Year Treasury", assetId = "ZN=F", description = "10 Year Treasury")
    )

    /**
     * Downloads the latest time series data
     */
    fun downloadTimeSeries() {
        timeSeriesDefinitions.forEach {
            definition ->
            yFinanceTimeSeriesDownloader.downloadHistoryForTicker(ticker = definition.assetId, interval = "1mo", range = "10y", save = true)
        }
    }

    fun getTimeSeriesDefinitions(): List<TimeSeriesDefinition> {
        return timeSeriesDefinitions
    }

}