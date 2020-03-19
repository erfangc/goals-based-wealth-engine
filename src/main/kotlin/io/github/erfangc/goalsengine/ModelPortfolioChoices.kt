package io.github.erfangc.goalsengine

import io.github.erfangc.analysis.AnalysisRequest
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.users.ModelPortfolio
import io.github.erfangc.users.ModelPortfolioSettings

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
class ModelPortfolioChoices(private val analysisService: AnalysisService, private val modelPortfolioSettings: ModelPortfolioSettings) : PortfolioChoices {

    private val analyses = modelPortfolioSettings
            .modelPortfolios
            .map { modelPortfolio -> modelPortfolio to analysisService.analyze(AnalysisRequest(listOf(modelPortfolio.portfolio))) }
            .toMap()

    override fun mus(): List<Double> {
        return analyses.map { it.value.analysis.expectedReturn }
    }

    override fun sigma(mu: Double): Double {
        return analyses
                .values
                .find { it.analysis.expectedReturn == mu }
                ?.analysis
                ?.volatility ?: error("Unable to find model portfolio with volatility corresponding to expected return $mu")
    }

    override fun muMax(): Double {
        return analyses
                .maxBy { it.value.analysis.expectedReturn }
                ?.value
                ?.analysis
                ?.expectedReturn ?: error("")
    }

    override fun muMin(): Double {
        return analyses
                .minBy { it.value.analysis.expectedReturn }
                ?.value
                ?.analysis
                ?.expectedReturn ?: error("")
    }

    fun getPortfolio(mu: Double): ModelPortfolio {
        return analyses.entries.find { it.value.analysis.expectedReturn == mu }?.key ?: error("")
    }
}