package io.github.erfangc.convexoptimizer

import ilog.concert.IloObjectiveSense
import ilog.cplex.IloCplex
import io.github.erfangc.assets.AssetService
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.users.UserService
import org.springframework.stereotype.Service

@Service
class ConvexOptimizerService(
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
        private val assetService: AssetService,
        private val userService: UserService
) {

    /**
     * For convex optimization there are two classes of objectives:
     *
     * 1 - We create the objective as minimize
     * portfolio risk given a variance covariance matrix
     *
     * Furthermore, we target an expected level of returns
     *
     * 2 - If a target (model) portfolio is provided, we minimize tracking error
     * to that of the model portfolio
     *
     */
    fun optimizePortfolio(req: OptimizePortfolioRequest): OptimizePortfolioResponse {
        val assetIds = assetIds(req)
        val assets = assetService.getAssets(assetIds).associateBy { it.assetId }

        val cplex = IloCplex()

        //
        // build the risk terms on which we minimize
        //
        val response = covarianceService.computeCovariances(assetIds)

        cplex.objective(IloObjectiveSense.Maximize, cplex.numVar(1.0,1.0))

        //
        // position level restrictions (tax etc.)
        //
        TODO()
    }

    private fun assetIds(req: OptimizePortfolioRequest): List<String> {
        val holdings = holdingAssetIds(req)
        val whiteList = whiteList()
        return (holdings + whiteList).distinct()
    }

    private fun whiteList(): List<String> {
        val user = userService.getUser()
        return user.overrides?.whiteList?.map { it.assetId } ?: emptyList()
    }

    private fun holdingAssetIds(req: OptimizePortfolioRequest): List<String> {
        return req
                .portfolios
                ?.flatMap { it.portfolio.positions.map { position -> position.assetId } }
                ?: emptyList()
    }

}