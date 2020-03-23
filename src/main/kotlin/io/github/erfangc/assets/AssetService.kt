package io.github.erfangc.assets

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes
import io.github.erfangc.util.DynamoDBUtil.fromItem
import org.springframework.stereotype.Service

@Service
class AssetService(private val ddb: AmazonDynamoDB) {

    fun getAssets(assetIds: List<String>): List<Asset> {
        // TODO figure out which assets were missing and populate accordingly
        return assetIds
                .distinct()
                .chunked(25)
                .flatMap { chunk ->
                    val tableName = "assets"
                    val attributeValues = KeysAndAttributes()
                            .withKeys(chunk.map<String, Map<String, AttributeValue>> { assetId -> mapOf("id" to AttributeValue(assetId)) })
                    val responses = ddb.batchGetItem(mapOf(tableName to attributeValues)).responses
                    responses[tableName]?.map { item -> fromItem<Asset>(item) } ?: emptyList()
                }
    }

    fun getAssetByCUSIP(cusip: String): Asset? {
        // we need to figure out a way to cheaply source CUSIP at least for Plaid import
        return null
    }

    fun getAssetByTicker(ticker: String): Asset? {
        return getAssets(listOf(ticker)).firstOrNull()
    }

}
