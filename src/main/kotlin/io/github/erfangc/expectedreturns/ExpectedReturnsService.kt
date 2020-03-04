package io.github.erfangc.expectedreturns

import org.springframework.stereotype.Service

@Service
class ExpectedReturnsService {
    /**
     * Compute the expected returns of assets given their id
     *
     * @return a Map whose keys are assetIds and the values are expected returns for the asset
     */
    fun getExpectedReturns(assetIds: List<String>): Map<String, Double> {
        TODO()
    }
}