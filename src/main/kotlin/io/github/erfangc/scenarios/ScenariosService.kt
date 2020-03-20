package io.github.erfangc.scenarios

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.util.DateUtils.mostRecentMonthEnd
import io.github.erfangc.util.DynamoDBUtil
import io.github.erfangc.util.PortfolioUtils.assetIds
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Performs regression based scenario analysis
 *
 * Scenario gains and losses at the individual asset level is computed
 * as the shock sizes * the regression beta between the indices being shocked in an scenario analysis
 * and the asset's own return time series
 *
 * TODO in a factor based model, asset returns are built up from factor returns
 */
@Service
class ScenariosService(private val assetTimeSeriesService: AssetTimeSeriesService,
                       private val ddb: AmazonDynamoDB) {

    fun scenariosAnalysis(req: ScenariosAnalysisRequest): ScenarioAnalysisResponse {
        val stop = mostRecentMonthEnd()
        val start = mostRecentMonthEnd()

        val timeSeriesIds = timeSeriesIds(req)
        val assetIds = assetIds(req.portfolios)
        val assetReturnTimeSeries = assetTimeSeriesService.getMonthlyReturnTimeSeries(assetIds, start, stop)
        TODO()
    }

    /**
     * fetches index / market time series from database
     */
    private fun getTimeSeries(
            timeSeriesIds: List<String>,
            start: LocalDate,
            stop: LocalDate
    ): Map<String, List<TimeSeriesDatum>> {
        val timeSeries = timeSeriesIds.flatMap { assetId ->
            val hashKeys = "timeSeriesId" to Condition()
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
                    QueryRequest("non-asset-time-series")
                            .withKeyConditions(mapOf(hashKeys, rangeKeys))
            ).items
            items.map { DynamoDBUtil.fromItem<TimeSeriesDatum>(it) }
        }
        return timeSeries.groupBy { it.timeSeriesId }
    }

    private fun timeSeriesIds(req: ScenariosAnalysisRequest): List<String> {
        return req.scenarioDefinitions.flatMap { scenarioDefinition ->
            scenarioDefinition.scenarioShocks.map { it.timeSeriesId }
        }.distinct()
    }

}


