package io.github.erfangc.goalsengine

import io.github.erfangc.convexoptimizer.ConvexOptimizerService
import io.github.erfangc.convexoptimizer.models.ConstrainedMeanVarianceOptimizationRequest
import io.github.erfangc.convexoptimizer.models.Objectives
import io.github.erfangc.convexoptimizer.models.PortfolioDefinition
import io.github.erfangc.goalsengine.models.ConstructEfficientFrontierRequest
import io.github.erfangc.goalsengine.models.ConstructEfficientFrontierResponse
import io.github.erfangc.goalsengine.models.Sample
import io.github.erfangc.marketvalueanalysis.MarketValueAnalysisService
import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysisRequest
import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.portfolios.models.Position
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.sqrt

/**
 * This is a Bean that can construct efficient frontiers
 * using a white list of assets and their corresponding expected return and variances
 *
 * This service is intended to be used externally as a standalone feature
 */
@Service
class EfficientFrontierService(
        private val convexOptimizerService: ConvexOptimizerService,
        private val marketValueAnalysisService: MarketValueAnalysisService
) {

    private val log = LoggerFactory.getLogger(EfficientFrontierService::class.java)
    private val executor = Executors.newFixedThreadPool(10)

    fun constructEfficientFrontier(req: ConstructEfficientFrontierRequest): ConstructEfficientFrontierResponse {
        val cash = Position(id = "CASH-USD", assetId = "USD", quantity = 100_000.0)
        val requestTemplate = ConstrainedMeanVarianceOptimizationRequest(
                portfolios = listOf(
                        PortfolioDefinition(
                                portfolio = Portfolio(id = "", positions = listOf(cash)),
                                whiteList = req.whiteList
                        )
                ),
                objectives = Objectives(expectedReturn = 0.02)
        )
        val requestAttributes = RequestContextHolder.currentRequestAttributes()
        val sampleFutures: List<Future<Sample?>> = (2..10).map { returnTarget ->
            val future: Future<Sample?> = executor.submit(Callable {
                RequestContextHolder.setRequestAttributes(requestAttributes)
                try {
                    val request = requestTemplate
                            .copy(objectives = requestTemplate.objectives.copy(expectedReturn = returnTarget.div(100.0)))
                    val optimizationResponse = convexOptimizerService.constrainedMeanVarianceOptimization(request)
                    // convert optimization outcome to a portfolio
                    val positions = optimizationResponse
                            .proposedOrders
                            .map { order ->
                                if (order.assetId == "USD") {
                                    Position(quantity = cash.quantity + order.quantity, assetId = order.assetId)
                                } else {
                                    Position(quantity = order.quantity, assetId = order.assetId)
                                }
                            }
                    val portfolio = Portfolio(positions = positions)
                    val objectiveValue = optimizationResponse.objectiveValue

                    // analyze said portfolio
                    val marketValueAnalysis = marketValueAnalysisService
                            .marketValueAnalysis(MarketValueAnalysisRequest(listOf(portfolio)))
                    val ret = Sample(
                            mu = returnTarget.div(100.0),
                            sigma = sqrt(objectiveValue),
                            portfolio = portfolio,
                            marketValueAnalysis = marketValueAnalysis
                    )
                    RequestContextHolder.resetRequestAttributes()
                    ret
                } catch (e: Exception) {
                    log.error("Unable to run convex optimization for returnTarget=${returnTarget}", e)
                    null
                }
            })
            future
        }
        val samples = sampleFutures.mapNotNull { it.get() }
        return ConstructEfficientFrontierResponse(samples = samples)
    }
}