package io.github.erfangc.assets.internal

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import org.apache.http.HttpHost
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class AssetsConfiguration {
    @Bean
    fun ddb(): AmazonDynamoDB {
        return AmazonDynamoDBClientBuilder.defaultClient()
    }

    @Bean
    fun httpClient(): HttpClient {
        return HttpClientBuilder.create().build()
    }

    @Bean
    fun restHighlevelClient(ssm: AWSSimpleSystemsManagement): RestHighLevelClient {
        val elasticsearchHost = System.getenv("ELASTICSEARCH_HOST") ?: ssm
                .getParameter(GetParameterRequest().withName("/wealth-engine/elasticsearch-host"))
                .parameter
                .value
        return RestHighLevelClient(
                RestClient.builder(
                        HttpHost(elasticsearchHost, 9200, "http")
                )
        )
    }
}