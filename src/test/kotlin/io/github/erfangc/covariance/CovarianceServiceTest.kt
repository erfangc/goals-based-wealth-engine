package io.github.erfangc.covariance

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.github.erfangc.assets.AssetTimeSeriesService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class CovarianceServiceTest {

    @Test
    fun computeCovariances() {
        val ddb = AmazonDynamoDBClientBuilder.defaultClient()
        val svc = CovarianceService(AssetTimeSeriesService(ddb = ddb))
        val response = svc.computeCovariances(
                assetIds = listOf("VTI", "VTI", "BND")
        )
    }
}