package io.github.erfangc.assets

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import io.github.erfangc.util.DynamoDBUtil.fromItem
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AssetTimeSeriesService(private val ddb: AmazonDynamoDB) {

    /**
     * Query DynamoDB
     */
    @Cacheable("monthly-return-time-series")
    fun getMonthlyReturnTimeSeries(assetIds: List<String>,
                                   start: LocalDate,
                                   stop: LocalDate): List<TimeSeriesDatum> {
        return assetIds.flatMap {
            assetId ->
            val hashKeys = "assetId" to Condition()
                    .withAttributeValueList(AttributeValue(assetId))
                    .withComparisonOperator(ComparisonOperator.EQ)
            val rangeKeys = "date" to Condition()
                    .withAttributeValueList(
                            listOf(
                                    AttributeValue(start.toString()),
                                    AttributeValue(stop.toString())
                            )
                    )
                    .withComparisonOperator(ComparisonOperator.BETWEEN)
            val items = ddb.query(
                    QueryRequest("asset-prices-history")
                            .withKeyConditions(mapOf(hashKeys, rangeKeys))
            ).items
            items.map { fromItem<TimeSeriesDatum>(it) }
        }
    }
}
