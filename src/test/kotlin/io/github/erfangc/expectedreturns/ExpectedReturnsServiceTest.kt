package io.github.erfangc.expectedreturns

import io.github.erfangc.assets.AssetService
import io.github.erfangc.assets.AssetTimeSeriesService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.slf4j.LoggerFactory

internal class ExpectedReturnsServiceTest {

    private val log = LoggerFactory.getLogger(ExpectedReturnsServiceTest::class.java)

    @Test
    fun getExpectedReturns() {
        val svc = ExpectedReturnsService(AssetTimeSeriesService(), AssetService())
        val expectedReturns = svc.getExpectedReturns(listOf("BND", "BNDX", "VTI", "VWO", "VEA"))
        expectedReturns.forEach { (assetId, expectedReturn) ->
            log.info("$assetId ${expectedReturn * 100}%")
        }
    }
}