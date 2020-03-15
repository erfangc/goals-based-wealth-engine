package io.github.erfangc.util

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.ItemUtils
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

object DynamoDBUtil {
    val objectMapper: ObjectMapper = jacksonObjectMapper()
            .findAndRegisterModules()
    inline fun <reified T> fromItem(item: Map<String, AttributeValue>): T {
        val json = ItemUtils.toItem(item).toJSON()
        return this.objectMapper.readValue(json)
    }
    fun toItem(t: Any): Map<String, AttributeValue> {
        val json = objectMapper.writeValueAsString(t)
        return ItemUtils.toAttributeValues(Item.fromJSON(json))
    }
}