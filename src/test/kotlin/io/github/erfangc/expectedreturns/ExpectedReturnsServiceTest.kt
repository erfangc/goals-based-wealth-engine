package io.github.erfangc.expectedreturns

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.github.erfangc.assets.AssetService
import io.github.erfangc.assets.AssetTimeSeriesService
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

internal class ExpectedReturnsServiceTest {

    private val log = LoggerFactory.getLogger(ExpectedReturnsServiceTest::class.java)

    @Test
    fun getExpectedReturns() {
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val svc = ExpectedReturnsService(AssetTimeSeriesService(ddb), AssetService(ddb))
        val expectedReturns = svc.getExpectedReturns(listOf("BND", "BNDX", "VTI", "VWO", "VEA"))
        expectedReturns.forEach { (assetId, expectedReturn) ->
            log.info("$assetId ${expectedReturn.expectedReturn * 100}%")
        }
    }
}