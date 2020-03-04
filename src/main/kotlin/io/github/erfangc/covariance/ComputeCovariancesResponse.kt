package io.github.erfangc.covariance

data class ComputeCovariancesResponse(val covariances: Array<DoubleArray>, val assetIndexLookup: Map<String, Int>)