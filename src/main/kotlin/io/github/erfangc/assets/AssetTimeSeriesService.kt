package io.github.erfangc.assets

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.streams.toList

@Service
class AssetTimeSeriesService {

    /**
     * Computes the return of a given list of assets
     * and return them in a single (co-mingled) list of time series datum objects
     *
     * The caller can use `groupBy` operations to separate time series by asset post call
     */
    fun getDailyReturnTimeSeries(assetIds: List<String>,
                                 start: LocalDate,
                                 stop: LocalDate): List<TimeSeriesDatum> {
        val returnTimeSeries = assetIds.map { assetId ->

            val reader = ClassPathResource("assets/time-series/$assetId.json")
                    .inputStream
                    .bufferedReader()

            val prices = reader
                    .lineSequence()
                    .drop(1)
                    .toList()
                    .map { line ->
                        val parts = line.split(",")
                        val date = LocalDate.parse(parts[0])
                        val adjClose = parts[5].toDouble()
                        TimeSeriesDatum(assetId = assetId, date = date, field = Field.PRICE, value = adjClose)
                    }

            // we compute returns by considering a moving window of size 2
            // we implement this moving window algorithm via a fold operation that
            assetId to prices.foldIndexed(emptyList<TimeSeriesDatum>()) { idx, acc, datum ->
                if (idx != 0) {
                    val prevClose = prices[idx - 1].value
                    val ret = datum.value / prevClose - 1
                    acc + datum.copy(field = Field.RETURN, value = ret)
                } else {
                    // for the very first element, do not compute a return since there is no prior day data
                    acc
                }
            }.associateBy { it.date }

        }.toMap()

        return start
                .datesUntil(stop.plusDays(1))
                .toList()
                .flatMap { date ->
                    assetIds.map { assetId ->
                        // we find the return time series datum or else return 0.0
                        returnTimeSeries[assetId]?.get(date)
                                ?: TimeSeriesDatum(assetId = assetId, date = date, field = Field.RETURN, value = 0.0)
                    }
                }
    }

    /**
     * Computes the monthly rather than daily return of a given list of assets
     * and return them in a single (co-mingled) list of time series datum objects
     *
     * The caller can use `groupBy` operations to separate time series by asset post call
     *
     * Monthly returns are usually used to find expected returns as the factor returns are usually computed
     * at this interval
     */
    fun getMonthlyReturnTimeSeries(assetIds: List<String>,
                                   start: LocalDate,
                                   stop: LocalDate): List<TimeSeriesDatum> {
        // first find the daily returns then take geometric averages
        return getDailyReturnTimeSeries(assetIds, start, stop)
                // first group by assetId, within each asset's daily return history group by again by year/month
                .groupBy { it.assetId }
                .flatMap { (assetId, v) ->
                    v.groupBy {
                        it.date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    }.map {
                        (date, dailyReturns) ->
                        val value = dailyReturns.fold(1.0) {
                            acc: Double, datum: TimeSeriesDatum ->
                            acc * (datum.value + 1.0)
                        } - 1.0
                        TimeSeriesDatum(
                                assetId = assetId,
                                value = value,
                                field = Field.RETURN,
                                date = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM"))
                        )
                    }
                }
    }
}