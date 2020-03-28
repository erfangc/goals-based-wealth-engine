package io.github.erfangc.assets.parser.yfinance

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.PutRequest
import com.amazonaws.services.dynamodbv2.model.WriteRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.erfangc.assets.Field
import io.github.erfangc.assets.TimeSeriesDatum
import io.github.erfangc.util.DynamoDBUtil.toItem
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneId

@Service
class YFinanceTimeSeriesDownloader(private val httpClient: HttpClient,
                                   private val objectMapper: ObjectMapper,
                                   private val ddb: AmazonDynamoDB
) {
    private val log = LoggerFactory.getLogger(YFinanceTimeSeriesDownloader::class.java)

    fun downloadHistoryForTicker(
            ticker: String,
            interval: String = "1mo",
            range: String = "max",
            save: Boolean = false
    ): List<TimeSeriesDatum> {
        val encodedTicker = URLEncoder.encode(ticker, Charset.defaultCharset())
        val params = listOf(
                "interval" to interval,
                "range" to range
        ).joinToString("&") { "${it.first}=${it.second}" }
        val jsonNode = httpClient.execute(HttpGet("https://query1.finance.yahoo.com/v8/finance/chart/$encodedTicker?$params")) { response ->
            val json = response.entity.content.bufferedReader().readText()
            objectMapper.readTree(json)
        }
        val adjCloseNodes = jsonNode.at("/chart/result/0/indicators/adjclose/0/adjclose")
        val adjCloses = adjCloseNodes.map { adjClose ->
            adjClose.asDouble()
        }
        val dates = jsonNode.at("/chart/result/0/timestamp").map { timestamp ->
            Instant.ofEpochSecond(timestamp.asLong()).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val monthlyPrices = dates.mapIndexed { index, localDate ->
            TimeSeriesDatum(
                    assetId = ticker,
                    date = localDate.toString(),
                    value = adjCloses[index],
                    field = Field.PRICE
            )
        }.sortedBy { it.date }
        val monthlyReturns = monthlyPrices
                .dropLast(1)
                .mapIndexedNotNull { t, datum ->
                    if (t == 0) {
                        null
                    } else {
                        val prev = monthlyPrices[t - 1]
                        val monthlyReturn = datum.value / prev.value - 1
                        datum.copy(value = monthlyReturn, field = Field.RETURN)
                    }
                }
        if (save) {
            val chunks = monthlyReturns.chunked(25)
            log.info("Writing time series for $ticker to database in ${chunks.size} chunks")
            val tableName = "asset-prices-history"
            chunks.forEachIndexed { idx, chunk ->
                val requests = chunk.map { timeSeriesDatum -> WriteRequest(PutRequest(toItem(timeSeriesDatum))) }
                val writeItemResult = ddb.batchWriteItem(mapOf(tableName to requests))
                if (writeItemResult.unprocessedItems[tableName].isNullOrEmpty()) {
                    log.info("Wrote chunk $idx with ${chunk.size} rows")
                } else {
                    val numFailed = writeItemResult.unprocessedItems[tableName]?.size
                    log.error("Failed to write $numFailed in chunk $idx")
                }
            }
        }
        return monthlyReturns
    }
}