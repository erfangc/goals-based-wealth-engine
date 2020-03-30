package io.github.erfangc.convexoptimizer.internal

import ilog.concert.IloNumVar
import ilog.cplex.IloCplex
import io.github.erfangc.assets.models.Asset
import io.github.erfangc.convexoptimizer.models.ConvexOptimizationRequest
import io.github.erfangc.convexoptimizer.models.PortfolioDefinition
import io.github.erfangc.expectedreturns.models.ExpectedReturn
import io.github.erfangc.marketvalueanalysis.models.MarketValueAnalysis

data class OptimizationContext(
        val cplex: IloCplex,
        val request: ConvexOptimizationRequest,
        val assets: Map<String, Asset>,
        val assetIds: List<String>,
        val assetVars: Map<String, IloNumVar>,
        val portfolioDefinitions: List<PortfolioDefinition>,
        val positionVars: List<PositionVar>,
        val expectedReturns: Map<String, ExpectedReturn>,
        val marketValueAnalyses: MarketValueAnalysis,
        val aggregateNav: Double
)