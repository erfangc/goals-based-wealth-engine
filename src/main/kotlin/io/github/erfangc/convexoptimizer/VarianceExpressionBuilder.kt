package io.github.erfangc.convexoptimizer

import ilog.concert.IloNumExpr
import ilog.cplex.IloCplex
import io.github.erfangc.analysis.AnalysisRequest
import io.github.erfangc.analysis.AnalysisService
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.portfolios.Portfolio

class VarianceExpressionBuilder(
        private val covarianceService: CovarianceService,
        private val analysisService: AnalysisService
) {

    fun varianceExpr(ctx: OptimizationContext): IloNumExpr {
        val request = ctx.request
        val modelPortfolio = request.modelPortfolio?.portfolio
        return if (modelPortfolio != null) {
            exprForTrackingError(modelPortfolio, ctx)
        } else {
            exprForVariance(ctx)
        }
    }

    private fun exprForTrackingError(modelPortfolio: Portfolio, ctx: OptimizationContext): IloNumExpr {
        val cplex = ctx.cplex
        val modelPositions = modelPortfolio.positions.associateBy { it.assetId }
        val (analysis, _) = analysisService.analyze(AnalysisRequest(portfolios = listOf(modelPortfolio)))
        val modelWeights = analysis.marketValueAnalysis.weights[modelPortfolio.id] ?: error("")

        val combinedAssetIds = (modelPositions.keys + ctx.assetIds).toList()
        val response = covarianceService.computeCovariances(combinedAssetIds)
        val covariances = response.covariances
        val assetIndexLookup = response.assetIndexLookup

        // tracking error terms short the model portfolio holdings
        // for every asset that is found in the model
        val trackingErrorTerms = combinedAssetIds.flatMap { a1 ->
            combinedAssetIds.map { a2 ->
                val a1Idx = assetIndexLookup[a1] ?: error("")
                val a2Idx = assetIndexLookup[a2] ?: error("")
                val covariance = covariances[a1Idx][a2Idx]
                val a1ModelWt = modelPositions[a1]?.id?.let { modelWeights[it] } ?: 0.0
                val a2ModelWt = modelPositions[a2]?.id?.let { modelWeights[it] } ?: 0.0
                val a1Var = ctx.assetVars[a1]?.let { assetVar -> cplex.sum(assetVar, -a1ModelWt) }
                        ?: cplex.constant(-a1ModelWt)
                val a2Var = ctx.assetVars[a2]?.let { assetVar -> cplex.sum(assetVar, -a2ModelWt) }
                        ?: cplex.constant(-a2ModelWt)
                cplex.prod(cplex.prod(a1Var, a2Var), covariance)
            }
        }.toTypedArray()
        return cplex.sum(trackingErrorTerms)
    }

    private fun exprForVariance(ctx: OptimizationContext): IloNumExpr {
        val cplex = ctx.cplex
        // build the risk terms on which we minimize
        // this works by iterating over the asset decision variables
        // in a double loop to emulate x∑x^T
        val response = covarianceService.computeCovariances(ctx.assetIds)
        val covariances = response.covariances
        val assetIndexLookup = response.assetIndexLookup
        val varianceTerms = ctx.assetIds.flatMap { a1 ->
            ctx.assetIds.map { a2 ->
                val a1Idx = assetIndexLookup[a1] ?: error("")
                val a2Idx = assetIndexLookup[a2] ?: error("")
                val covariance = covariances[a1Idx][a2Idx]
                val a1Var = ctx.assetVars[a1] ?: error("")
                val a2Var = ctx.assetVars[a2] ?: error("")
                cplex.prod(a1Var, a2Var, covariance)
            }
        }.toTypedArray()
        return cplex.sum(varianceTerms)
    }

}