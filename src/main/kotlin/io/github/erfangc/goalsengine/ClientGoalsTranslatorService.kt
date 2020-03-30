package io.github.erfangc.goalsengine

import io.github.erfangc.goalsengine.models.Cashflow
import io.github.erfangc.goalsengine.models.TranslateClientGoalsRequest
import io.github.erfangc.goalsengine.models.TranslateClientGoalsResponse
import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysisRequest
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.pow

@Service
class ClientGoalsTranslatorService(private val marketValueAnalysisService: MarketValueAnalysisService) {

    fun translate(req: TranslateClientGoalsRequest): TranslateClientGoalsResponse {
        return TranslateClientGoalsResponse(
                goal = goal(req),
                initialInvestment = initialInvestment(req),
                investmentHorizon = investmentHorizon(req),
                cashflows = cashflows(req)
        )
    }

    private fun initialInvestment(req: TranslateClientGoalsRequest): Double {
        return marketValueAnalysisService
                .marketValueAnalysis(MarketValueAnalysisRequest(req.portfolios))
                .marketValueAnalysis
                .netAssetValue
    }

    /**
     * Derive the goal (lump sum) amount to target at retirement
     */
    private fun goal(req: TranslateClientGoalsRequest): Double {
        val goals = req.client.goals ?: error("")
        val requiredIncome = goals.retirementYearlyIncome - goals.supplementalYearlyIncome
        // TODO we need to come up with assumptions and calculations for decumulation so we can compute the lump sum
        val n = 30
        val r = 0.03
        return requiredIncome * ((1 - (1 / (1 + r).pow(n))) / r)
    }

    /**
     * Derive investment horizon based on client goals
     */
    private fun investmentHorizon(req: TranslateClientGoalsRequest): Int {
        val retirementYear = req.client.goals?.retirement ?: error("")
        val year = LocalDate.now().year
        return retirementYear.year - year
    }

    /**
     * Simple conversion of known cashflows from date vs. year until format
     */
    private fun cashflows(req: TranslateClientGoalsRequest): List<Cashflow> {
        val year = LocalDate.now().year
        return req
                .client
                .goals
                ?.knownCashflows
                ?.map { Cashflow(t = it.year - year, amount = it.amount) }
                ?: error("")
    }

}
