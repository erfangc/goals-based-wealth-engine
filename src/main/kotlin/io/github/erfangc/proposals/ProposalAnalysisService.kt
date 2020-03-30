package io.github.erfangc.proposals

import io.github.erfangc.analysis.models.AnalysisRequest
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.portfolios.models.Position
import io.github.erfangc.proposals.models.*
import org.springframework.stereotype.Service

@Service
class ProposalAnalysisService(private val analysisService: AnalysisService) {

    fun analyze(req: AnalyzeProposalRequest): AnalyzeProposalResponse {

        val original = req.proposal.portfolios
        val proposedPortfolios = proposedPortfolios(req.proposal)

        val originalAnalysisResponse = analysisService.analyze(AnalysisRequest(portfolios = original, client = req.client))
        val proposedAnalysisResponse = analysisService.analyze(AnalysisRequest(portfolios = proposedPortfolios, client = req.client))

        val oAnalysis = originalAnalysisResponse.analysis
        val pAnalysis = proposedAnalysisResponse.analysis

        val assets = (originalAnalysisResponse.assets.keys + proposedAnalysisResponse.assets.keys)
                .distinct()
                .map { assetId ->
                    val asset = originalAnalysisResponse.assets[assetId]
                            ?: proposedAnalysisResponse.assets[assetId]
                            ?: error("")
                    assetId to asset
                }
                .toMap()

        return AnalyzeProposalResponse(
                analyses = Analyses(
                        expectedReturns = ExpectedReturns(
                                original = oAnalysis.expectedReturn,
                                proposed = pAnalysis.expectedReturn
                        ),
                        weights = Weights(
                                original = oAnalysis.marketValueAnalysis.weights,
                                proposed = pAnalysis.marketValueAnalysis.weights
                        ),
                        marketValue = MarketValue(
                                original = oAnalysis.marketValueAnalysis.marketValue,
                                proposed = pAnalysis.marketValueAnalysis.marketValue
                        ),
                        volatility = Volatility(
                                original = oAnalysis.volatility,
                                proposed = pAnalysis.volatility
                        ),
                        probabilityOfSuccess = ProbabilityOfSuccess(
                                original = oAnalysis.probabilityOfSuccess,
                                proposed = pAnalysis.probabilityOfSuccess
                        ),
                        allocations = GenerateProposalResponseAllocations(
                                original = oAnalysis.marketValueAnalysis.allocations,
                                proposed = pAnalysis.marketValueAnalysis.allocations
                        ),
                        netAssetValue = NetAssetValue(
                                original = oAnalysis.marketValueAnalysis.netAssetValue,
                                proposed = oAnalysis.marketValueAnalysis.netAssetValue
                        ),
                        scenarioOutputs = ScenarioOutputs(
                                original = oAnalysis.scenarioOutputs,
                                proposed = pAnalysis.scenarioOutputs
                        ),
                        simulatedPerformances = SimulatedPerformances(
                                original = SimulatedPerformance(oAnalysis.simulatedPerformance, oAnalysis.simulatedPerformanceSummaryMetrics),
                                proposed = SimulatedPerformance(pAnalysis.simulatedPerformance, pAnalysis.simulatedPerformanceSummaryMetrics)
                        )
                ),
                proposedPortfolios = proposedPortfolios,
                assets = assets
        )

    }

    private fun proposedPortfolios(proposal: Proposal): List<Portfolio> {
        val orders = proposal.proposedOrders
                .groupBy { it.portfolioId }
                .mapValues { it.value.associateBy { order -> order.positionId } }

        // overlay the existing portfolio definitions (which includes any new portfolio created) with orders
        return proposal.portfolios.map { portfolio ->
            val portfolioId = portfolio.id
            val portfolioOrders = orders[portfolioId] ?: emptyMap()
            val updatedExistingPositions = portfolio.positions.map { position ->
                val positionId = position.id
                val order = portfolioOrders[positionId]
                position.copy(quantity = position.quantity + (order?.quantity ?: 0.0))
            }
            // append any new positions
            val newPositions = portfolioOrders.keys.subtract(updatedExistingPositions.map { it.id }).map { positionId ->
                val order = portfolioOrders[positionId] ?: error("")
                Position(
                        id = positionId,
                        quantity = order.quantity,
                        assetId = order.assetId
                )
            }
            portfolio.copy(positions = updatedExistingPositions + newPositions)
        }
    }

}
