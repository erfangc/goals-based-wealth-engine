package io.github.erfangc.analysis

import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysis
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisResponse
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.portfolios.Position
import org.springframework.stereotype.Service
import kotlin.math.sqrt

@Service
class AnalysisService(
        private val marketValueAnalysisService: MarketValueAnalysisService,
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService
) {

    fun analyze(req: AnalysisRequest): AnalysisResponse {
        // flatten the group of portfolios into a single one
        val positions = mergePositions(req)
        val marketValueAnalysisResponse = marketValueAnalysis(positions)

        // compute variance
        val assetIds = positions.map { it.assetId }.distinct()
        val marketValueAnalysis = marketValueAnalysisResponse.marketValueAnalysis
        val expectedReturn = expectedReturn(positions, assetIds, marketValueAnalysis)
        val volatility = volatility(assetIds, positions, marketValueAnalysis)

        return AnalysisResponse(
                Analysis(
                        marketValueAnalysis = marketValueAnalysis,
                        expectedReturn = expectedReturn,
                        volatility = volatility
                ),
                assets = marketValueAnalysisResponse.assets
        )
    }

    private fun volatility(assetIds: List<String>, positions: List<Position>, marketValueAnalysis: MarketValueAnalysis): Double {
        val (covariances, assetIndexLookup) = covarianceService.computeCovariances(assetIds)
        val variance = positions.flatMap { p1 ->
            val a1 = assetIndexLookup[p1.assetId] ?: error("")
            positions.map { p2 ->
                val a2 = assetIndexLookup[p2.assetId] ?: error("")
                val w1 = marketValueAnalysis.weights[p1.assetId] ?: error("")
                val w2 = marketValueAnalysis.weights[p2.assetId] ?: error("")
                w1 * w2 * covariances[a1][a2]
            }
        }.sum()
        return sqrt(variance)
    }

    private fun expectedReturn(positions: List<Position>,
                               assetIds: List<String>,
                               marketValueAnalysis: MarketValueAnalysis): Double {
        // compute expected returns
        val expectedReturns = expectedReturnsService.getExpectedReturns(assetIds)
        return positions.sumByDouble { position ->
            val er = expectedReturns[position.assetId] ?: 0.0
            val wt = marketValueAnalysis.weights[position.id] ?: 0.0
            er * wt
        }
    }

    private fun mergePositions(req: AnalysisRequest): List<Position> {
        return req
                .portfolios
                .flatMap { it.positions }
                .groupBy { it.assetId }.map { (assetId, positions) ->
                    val quantity = positions.sumByDouble { it.quantity }
                    Position(
                            id = assetId,
                            assetId = assetId,
                            quantity = quantity
                    )
                }
    }

    private fun marketValueAnalysis(positions: List<Position>): MarketValueAnalysisResponse {
        return marketValueAnalysisService.marketValueAnalysis(MarketValueAnalysisRequest(Portfolio(
                id = "",
                positions = positions
        )))
    }

}