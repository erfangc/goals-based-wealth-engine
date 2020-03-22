package io.github.erfangc.assets

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.util.DynamoDBUtil.fromItem
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SearchAssetService(private val restHighLevelClient: RestHighLevelClient,
                         private val objectMapper: ObjectMapper,
                         private val ddb: AmazonDynamoDB) {

    private val log = LoggerFactory.getLogger(SearchAssetService::class.java)

    /**
     * Search Elasticsearch for "term" and return all assets that match
     */
    fun search(term: String): List<Asset> {
        val searchRequest = SearchRequest("assets")
                .source(SearchSourceBuilder
                        .searchSource()
                        .query(
                                BoolQueryBuilder()
                                        .should(MatchPhrasePrefixQueryBuilder("ticker", term))
                        )
                )
        val hits = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT).hits
        return hits.hits.map {
            objectMapper.readValue<Asset>(it.sourceAsString)
        }
    }

    fun syncWithDynamoDB() {
        // scan DynamoDB to retrieve all rows, populate those rows into Elasticsearch
        var lastEvaluatedKey: Map<String, AttributeValue>? = null
        log.info("Starting to scan DynamoDB for assets")
        do {
            val scanRequest = ScanRequest("assets")
                    .withExclusiveStartKey(lastEvaluatedKey)
            val scanResult = ddb.scan(scanRequest)
            val bulkRequest = BulkRequest()
            val items = scanResult.items
            lastEvaluatedKey = scanResult.lastEvaluatedKey
            lastEvaluatedKey?.let {
                log.info("Found ${items.size} items in DynamoDB lastEvaluatedKey=${lastEvaluatedKey["id"]}")
            }
            items.map { item ->
                try {
                    val asset = fromItem<Asset>(item)
                    val json = objectMapper.writeValueAsString(asset)
                    val indexRequest = IndexRequest("assets")
                            .id(asset.id)
                            .source(json, XContentType.JSON)
                    bulkRequest.add(indexRequest)
                } catch (e: Exception) {
                    log.error("Unable to deserialize Asset into an IndexRequest", e)
                }
            }
            log.info("Sending bulk request to Elasticsearch")
            try {
                restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT)
            } catch (e: Exception) {
                log.error("Elasticsearch bulk operation failed", e)
            }
            log.info("Finished bulk request to Elasticsearch")
        } while (lastEvaluatedKey != null && lastEvaluatedKey.isNotEmpty())
    }

}