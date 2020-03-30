package io.github.erfangc.ddb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * Creates the DynamoDB tables necessary for the operation of this App
 * if the table already exists the code will ensure that the table's keys & indices are setup in accordance
 * to the requirement of the latest version of the App or fail to start otherwise
 */
@Service
class DDBProvisioner(private val ddb: AmazonDynamoDB) {

    private val log = LoggerFactory.getLogger(DDBProvisioner::class.java)

    @PostConstruct
    fun init() {
        //
        // assets and time series table
        //
        createTableWithHashKeyOnly("assets")
        createTableWithHashAndRangeKey(
                tableName = "asset-returns",
                hashAttrName = "assetId",
                rangeAttrName = "date"
        )

        //
        // create tables to hold client / portfolios and users etc.
        //
        createTableWithHashKeyOnly("users")
        createTableWithHashKeyOnly("clients")
        createTableWithHashKeyOnly("portfolios")
    }

    private fun createTableWithHashAndRangeKey(tableName: String,
                                               rangeAttrName: String,
                                               hashAttrName: String,
                                               billingMode: BillingMode = BillingMode.PAY_PER_REQUEST
    ) {
        try {
            val describeTableResult = ddb.describeTable(tableName)
            val keySchemas = describeTableResult.table.keySchema
            if (!keySchemas.any { it.attributeName == rangeAttrName && it.keyType == "RANGE" }) {
                log.error("Table $tableName does not have a range key $rangeAttrName")
            }
            if (!keySchemas.any { it.attributeName == hashAttrName && it.keyType == "HASH" }) {
                log.error("Table $tableName does not have a hash key $hashAttrName")
            }
        } catch (e: ResourceNotFoundException) {
            log.info("Table $tableName does not exist, this App will use AWS SDK to create it")
            ddb.createTable(
                    CreateTableRequest(
                            tableName,
                            listOf(KeySchemaElement(hashAttrName, KeyType.HASH), KeySchemaElement("date", KeyType.RANGE)))
                            .withAttributeDefinitions(
                                    AttributeDefinition(hashAttrName, ScalarAttributeType.S),
                                    AttributeDefinition(rangeAttrName, ScalarAttributeType.S)
                            )
                            .withBillingMode(billingMode)
            )
            log.info("Created table $tableName")
        }
    }

    private fun createTableWithHashKeyOnly(tableName: String, idAttrName: String = "id", billingMode: BillingMode = BillingMode.PAY_PER_REQUEST) {
        try {
            val describeTableResult = ddb.describeTable(tableName)
            val keySchemas = describeTableResult.table.keySchema
            if (!keySchemas.any { it.attributeName == idAttrName && it.keyType == "HASH" }) {
                log.error("Table $tableName does not have a hash key $idAttrName")
            }
        } catch (e: ResourceNotFoundException) {
            log.info("Table $tableName does not exist, this App will use AWS SDK to create it")
            ddb.createTable(
                    CreateTableRequest(
                            tableName,
                            listOf(KeySchemaElement(idAttrName, KeyType.HASH)))
                            .withAttributeDefinitions(AttributeDefinition(idAttrName, ScalarAttributeType.S))
                            .withBillingMode(billingMode)
            )
            log.info("Created table $tableName")
        }
    }

}