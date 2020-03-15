package io.github.erfangc.assets

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
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
}