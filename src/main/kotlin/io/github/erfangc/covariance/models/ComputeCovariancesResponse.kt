package io.github.erfangc.covariance.models

data class ComputeCovariancesResponse(val covariances: Array<DoubleArray>, val assetIndexLookup: Map<String, Int>)