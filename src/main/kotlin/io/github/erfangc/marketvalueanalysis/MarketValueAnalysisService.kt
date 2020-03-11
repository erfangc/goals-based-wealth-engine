package io.github.erfangc.marketvalueanalysis

import io.github.erfangc.assets.Asset
import io.github.erfangc.assets.AssetService
import io.github.erfangc.portfolios.Position
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

        val portfolio = req.portfolio

        val assets = assetService
                .getAssets(portfolio.positions.map { it.assetId })
                .associateBy { it.assetId }

        val netAssetValue = portfolio.positions.fold(0.0) { acc, position ->
            acc + getMarketValue(position = position, asset = assets[position.assetId])
        }

        val marketValue = portfolio.positions.map { position ->
            val asset = assets[position.assetId]
            val marketValue = getMarketValue(asset, position)
            position.id to marketValue
        }.toMap()

        val weights = portfolio.positions.map { position ->
            position.id to (marketValue[position.id] ?: 0.0) / netAssetValue
        }.toMap()

        // create asset Allocation
        val buckets = portfolio
                .positions
                .groupBy { position ->
                    val asset = assets[position.assetId]
                    asset?.assetClass ?: "Other"
                }
                .map { (assetClass, positions) ->
                    val weight = positions.fold(0.0) { acc, position ->
                        val positionId = position.id
                        val weight = weights[positionId] ?: 0.0
                        acc + weight
                    }
                    Bucket(name = assetClass, weight = weight)
                }

        val allocations = Allocations(
                assetAllocation = Allocation(buckets = buckets)
        )

        return MarketValueAnalysisResponse(
                MarketValueAnalysis(netAssetValue, marketValue, weights, allocations)
        )
    }

}

