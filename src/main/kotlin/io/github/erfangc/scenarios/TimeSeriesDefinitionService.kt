package io.github.erfangc.scenarios

import io.github.erfangc.assets.parser.yfinance.YFinanceTimeSeriesDownloader
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class TimeSeriesDefinitionService(
        private val jdbcTemplate: NamedParameterJdbcTemplate,
        private val yFinanceTimeSeriesDownloader: YFinanceTimeSeriesDownloader
) {

    private val log = LoggerFactory.getLogger(TimeSeriesDefinitionService::class.java)

    fun downloadTimeSeries() {
        jdbcTemplate
                .queryForList("SELECT * FROM timeseriesdefinitions", mapOf<String, String>())
                .forEach {
                    row ->
                    row["assetId"]?.toString()?.let {
                        assetId -> yFinanceTimeSeriesDownloader.downloadHistoryForTicker(ticker = assetId, interval = "1mo", range = "10y", save = true)
                    }
                }
    }

    @PostConstruct
    fun init() {
        log.info("Uploading the latest time series definitions to the database")
        listOf(
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
        ).forEach { (id, name, assetId, url, description) ->
            log.info("Upserting time series definition definition id=$id name=$name")
            //language=PostgreSQL
            jdbcTemplate.update(
                    """
                       INSERT INTO 
                       timeSeriesDefinitions (id, name, assetId, url, description)
                       VALUES (:id, :name, :assetId, :url, :description)
                       ON CONFLICT DO NOTHING ; 
                    """.trimIndent(),
                    mapOf(
                            "id" to id,
                            "name" to name,
                            "assetId" to assetId,
                            "url" to url,
                            "description" to description
                    )
            )
        }
    }

}