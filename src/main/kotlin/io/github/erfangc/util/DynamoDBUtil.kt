package io.github.erfangc.util

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.ItemUtils
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object DynamoDBUtil {
    private val objectMapper = jacksonObjectMapper()
            .findAndRegisterModules()
    fun Map<String, AttributeValue>.toJson(): String {
        return ItemUtils.toItem(this).toJSON()
    }
    fun toItem(t: Any): Map<String, AttributeValue> {
        val json = objectMapper.writeValueAsString(t)
        return ItemUtils.toAttributeValues(Item.fromJSON(json))
    }
}