package io.github.erfangc.common

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.ItemUtils
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

object DynamoDBUtil {
    val objectMapper: ObjectMapper = jacksonObjectMapper()
            .findAndRegisterModules()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    inline fun <reified T> fromItem(item: Map<String, AttributeValue>): T {
        val json = ItemUtils.toItem(item).toJSON()
        return objectMapper.readValue(json)
    }
    fun toItem(t: Any): Map<String, AttributeValue> {
        val json = objectMapper.writeValueAsString(t)
        return ItemUtils.toAttributeValues(Item.fromJSON(json))
    }
}