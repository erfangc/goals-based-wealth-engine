package io.github.erfangc.convexoptimizer

import ilog.concert.IloNumExpr
import io.github.erfangc.covariance.CovarianceService

class VarianceExpressionBuilder(private val covarianceService: CovarianceService) {

    fun varianceExpr(ctx: OptimizationContext): IloNumExpr {
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