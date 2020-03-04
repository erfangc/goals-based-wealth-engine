package io.github.erfangc.assets

import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AssetTimeSeriesService {
    fun getAssetTimeSeries(assetIds: List<String>, start: Instant, stop: Instant): List<AssetTimeSeriesDatum> {
        TODO()
    }
}