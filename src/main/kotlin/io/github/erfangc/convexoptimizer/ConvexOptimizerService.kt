package io.github.erfangc.convexoptimizer

import ilog.cplex.IloCplex
import io.github.erfangc.covariance.CovarianceService
import io.github.erfangc.expectedreturns.ExpectedReturnsService
import io.github.erfangc.users.UserService
import org.springframework.stereotype.Service

@Service
class ConvexOptimizerService(
        private val expectedReturnsService: ExpectedReturnsService,
        private val covarianceService: CovarianceService,
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

        //
        // build the risk terms on which we minimize
        //
        val response = covarianceService.computeCovariances(assetIds)

        //
        // position level restrictions (tax)
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