package io.github.erfangc.assets

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class SearchAssetServiceTest {

    @Test
    fun syncWithDynamoDB() {
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val restHighLevelClient = RestHighLevelClient(
                RestClient.builder(
                        HttpHost("localhost", 9200, "http")
                )
        )
        val svc = SearchAssetService(
                ddb = AmazonDynamoDBClientBuilder.defaultClient(),
                objectMapper = objectMapper,
                restHighLevelClient = restHighLevelClient
        )
        svc.syncWithDynamoDB()
    }

}