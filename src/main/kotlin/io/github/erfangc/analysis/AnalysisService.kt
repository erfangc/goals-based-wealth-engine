package io.github.erfangc.analysis

import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import org.springframework.stereotype.Service

@Service
class AnalysisService(
        private val marketValueAnalysisService: MarketValueAnalysisService,
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService
) {
    fun analyze(req: AnalysisRequest): AnalysisResponse {
        TODO()
    }
}