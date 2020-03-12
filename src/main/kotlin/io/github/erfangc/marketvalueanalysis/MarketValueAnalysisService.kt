package io.github.erfangc.marketvalueanalysis

import io.github.erfangc.assets.Asset
import io.github.erfangc.assets.AssetService
import io.github.erfangc.portfolios.Position
import io.github.erfangc.util.PortfolioUtils.assetIds
import org.springframework.stereotype.Service

@Service
class MarketValueAnalysisService(private val assetService: AssetService) {

    private fun getMarketValue(asset: Asset?, position: Position): Double {
        return (asset?.price ?: 0.0) * position.quantity
    }

    /**
     * Market value analysis computes weight / market value of each position
     * as well as NAV of the portfolio and allocations
     */
    fun marketValueAnalysis(req: MarketValueAnalysisRequest): MarketValueAnalysisResponse {

        val portfolios = req.portfolios

        val assets = assetService
                .getAssets(assetIds(portfolios))
                .associateBy { it.assetId }

        val netAssetValues = portfolios.map { portfolio ->
            portfolio.id to portfolio.positions.fold(0.0) { acc, position ->
                acc + getMarketValue(position = position, asset = assets[position.assetId])
            }
        }.toMap()

        val netAssetValue = netAssetValues.values.sum()

        val marketValue = portfolios.map { portfolio ->
            portfolio.id to portfolio.positions.map { position ->
                val asset = assets[position.assetId]
                val marketValue = getMarketValue(asset, position)
                position.id to marketValue
            }.toMap()
        }.toMap()

        val weights = portfolios.map { portfolio ->
            portfolio.id to portfolio.positions.map { position ->
                val netAssetValue = netAssetValues[portfolio.id] ?: 0.0
                position.id to netAssetValue.let {
                    (marketValue[portfolio.id]?.get(position.id) ?: 0.0) / netAssetValue
                }
            }.toMap()
        }.toMap()

        // create asset Allocation
        val buckets = portfolios
                .flatMap { portfolio ->
                    val portfolioId = portfolio.id
                    portfolio
                            .positions
                            .map { position ->
                                val positionId = position.id
                                val assetId = position.assetId
                                val asset = assets[assetId]
                                val assetClass = asset?.assetClass ?: "Other"
                                val weight = (weights[portfolioId]?.get(positionId) ?: 0.0)
                                assetClass to weight
                            }
                }
                .groupBy { it.first }
                .map { (assetClass, weights) ->
                    Bucket(name = assetClass, weight = weights.sumByDouble { it.second })
                }

        val allocations = Allocations(
                assetAllocation = Allocation(buckets = buckets)
        )

        return MarketValueAnalysisResponse(
                MarketValueAnalysis(
                        netAssetValue = netAssetValue,
                        netAssetValues = netAssetValues,
                        weights = weights,
                        allocations = allocations,
                        marketValue = marketValue
                ),
                assets
        )
    }

}
