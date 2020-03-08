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

    fun analyze(req: MarketValueAnalysisRequest): MarketValueAnalysis {
        val portfolio = req.portfolio
        val assets = assetService
                .getAssets(portfolio.positions.map { it.assetId })
                .associateBy { it.assetId }

        val netAssetValue = portfolio.positions.fold(0.0) {
            acc, position ->
            acc + getMarketValue(position = position, asset = assets[position.assetId])
        }

        val marketValue = portfolio.positions.map { position ->
            val asset = assets[position.assetId]
            val marketValue = getMarketValue(asset, position)
            position.id to marketValue
        }.toMap()

        val weights = portfolio.positions.map { position ->
            position.id to (marketValue[position.id]?:0.0) / netAssetValue
        }.toMap()

        return MarketValueAnalysis(
                netAssetValue, marketValue, weights
        )
    }

}