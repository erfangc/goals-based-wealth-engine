package io.github.erfangc.covariance

import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.util.DateUtils.months
import org.apache.commons.math3.stat.correlation.Covariance
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CovarianceService(private val assetTimeSeriesService: AssetTimeSeriesService) {

    /**
     * Computes a covariance matrix for a given set of assetIds
     *
     * @return a ComputeCovariancesResponse instance which contains the covariance matrix
     * as an array and a look up map from the assetId to the index of that asset in the covariance matrix's
     * rows and column structure
     */
    fun computeCovariances(assetIds: List<String>): ComputeCovariancesResponse {
        // set to final date to the most recent month end
        val now = LocalDate.now()
        val lastMonth = now.minusMonths(1)
        // this should be the last day of last month
        val stop = lastMonth.minusDays(lastMonth.dayOfMonth.toLong() - 1)
        val start = stop.minusYears(5)
        val monthlyReturns = assetTimeSeriesService
                .getMonthlyReturnTimeSeries(assetIds, start, stop)
                .groupBy { it.assetId }
                .mapValues { it.value.associateBy { datum -> datum.date } }

        val months = months(start, stop)
        val data = months.map { date ->
            assetIds.map { assetId ->
                monthlyReturns[assetId]?.get(date)?.value ?: 0.0
            }.toDoubleArray()
        }.toTypedArray()
        val covariances = Covariance(data).covarianceMatrix.data

        return ComputeCovariancesResponse(
                covariances = covariances,
                assetIndexLookup = assetIds.mapIndexed { index, s -> s to index }.toMap()
        )
    }
}
