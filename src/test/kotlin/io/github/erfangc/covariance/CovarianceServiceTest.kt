package io.github.erfangc.covariance

import io.github.erfangc.assets.AssetTimeSeriesService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class CovarianceServiceTest {

    @Test
    fun computeCovariances() {
        val svc = CovarianceService(AssetTimeSeriesService())
        val response = svc.computeCovariances(
                assetIds = listOf("VTI", "VTI", "BND")
        )
        response
    }
}