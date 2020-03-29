package io.github.erfangc.simulatedperformance

import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.simulatedperformance.models.SimulatedPerformanceRequest
import io.github.erfangc.simulatedperformance.models.SimulatedPerformanceResponse
import org.springframework.stereotype.Service

@Service
class SimulatedPerformanceService(
        private val assetTimeSeriesService: AssetTimeSeriesService,
        private val marketValueAnalysisService: MarketValueAnalysisService
) {
    fun analyze(req: SimulatedPerformanceRequest): SimulatedPerformanceResponse {
        val (netAssetValue, netAssetValues, _, weights, _) = marketValueAnalysisService.marketValueAnalysis(MarketValueAnalysisRequest(req.portfolios)).marketValueAnalysis
        // aggregate all the positions
        TODO()
    }
}
