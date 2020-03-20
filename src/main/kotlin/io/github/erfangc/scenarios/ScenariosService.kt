package io.github.erfangc.scenarios

import io.github.erfangc.assets.AssetTimeSeriesService
import io.github.erfangc.util.PortfolioUtils.assetIds
import org.springframework.stereotype.Service

/**
 * Performs regression based scenario analysis
 *
 * Scenario gains and losses at the individual asset level is computed
 * as the shock sizes * the regression beta between the indices being shocked in an scenario analysis
 * and the asset's own return time series
 *
 * TODO in a factor based model, asset returns are built up from factor returns
 */
@Service
class ScenariosService(private val assetTimeSeriesService: AssetTimeSeriesService) {
    fun scenariosAnalysis(req: ScenariosAnalysisRequest): ScenarioAnalysisResponse {
        val assetIds = assetIds(req.portfolios)
//        val timeSeries = assetTimeSeriesService.getMonthlyReturnTimeSeries(assetIds)
        TODO()
    }
}


