package io.github.erfangc.goalsengine.models

import io.github.erfangc.analysis.models.AnalysisRequest
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.users.models.ModelPortfolio

/**
 * This [PortfolioChoices] implementation works to produce
 * the expected return and volatility of model portfolios
 *
 * The portfolio choice set is a set of model portfolios that the user have defined for themselves (with fallback to a set of default models)
 *
 * This is a sharply different from the [EfficientFrontier] approach, where investable assets are the input
 * and there are infinite combinations of mu/sigma pairs (along the efficient frontier). Here the possible mu/sigmas pairs
 * are limited the number of model portfolios
 */
class ModelPortfolioChoices(private val analysisService: AnalysisService,
                            modelPortfolios: List<ModelPortfolio>) : PortfolioChoices {

    private val analyses = modelPortfolios
            .map { modelPortfolio -> modelPortfolio to analysisService.analyze(AnalysisRequest(listOf(modelPortfolio.portfolio))) }
            .toMap()

    private val mus = analyses.map { it.value.analysis.expectedReturn }

    private val sigmaLookup = analyses
            .values
            .map { it.analysis.expectedReturn to it.analysis.volatility }
            .toMap()

    private val muMax = analyses
            .maxBy { it.value.analysis.expectedReturn }
            ?.value
            ?.analysis
            ?.expectedReturn ?: error("")

    private val muMin = analyses
            .minBy { it.value.analysis.expectedReturn }
            ?.value
            ?.analysis
            ?.expectedReturn ?: error("")

    override fun mus(): List<Double> {
        return mus
    }

    override fun sigma(mu: Double): Double {
        return sigmaLookup[mu] ?: error("Unable to find model portfolio with volatility corresponding to expected return $mu")
    }

    override fun muMax(): Double {
        return muMax
    }

    override fun muMin(): Double {
        return muMin
    }

    fun getPortfolio(mu: Double): ModelPortfolio {
        return analyses.entries.find { it.value.analysis.expectedReturn == mu }?.key ?: error("")
    }
}