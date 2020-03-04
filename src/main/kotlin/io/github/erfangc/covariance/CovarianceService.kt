package io.github.erfangc.covariance

import org.springframework.stereotype.Service

@Service
class CovarianceService {
    /**
     * Computes a covariance matrix for a given set of assetIds
     *
     * @return a ComputeCovariancesResponse instance which contains the covariance matrix
     * as an array and a look up map from the assetId to the index of that asset in the covariance matrix's
     * rows and column structure
     */
    fun computeCovariances(assetIds: List<String>): ComputeCovariancesResponse {
        TODO()
    }
}
