package io.github.erfangc.expectedreturns

import io.github.erfangc.assets.AssetTimeSeriesService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ExpectedReturnsServiceTest {

    @Test
    fun getExpectedReturns() {
        val svc = ExpectedReturnsService(AssetTimeSeriesService())
        val expectedReturns = svc.getExpectedReturns(listOf("BND", "VEA"))
        println(expectedReturns)
    }
}