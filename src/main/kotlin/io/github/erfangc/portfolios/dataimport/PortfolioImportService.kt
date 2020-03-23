package io.github.erfangc.portfolios.dataimport

import io.github.erfangc.assets.Asset
import io.github.erfangc.assets.AssetService
import io.github.erfangc.portfolios.Position
import org.springframework.stereotype.Service

@Service
class PortfolioImportService(private val assetService: AssetService) {

    companion object {
        const val fixedNav = 1_000_000.0
    }

    /**
     * Takes in a set of parsed pairs of id -> amount, parse them into a full portfolio
     * For now we just support resolution by ticker
     */
    fun importPortfolio(req: ImportPortfolioRequest): ImportPortfolioResponse {
        val lines = req.clipboard.split("\n")
        val delimiters = req.delimiter

        val hasHeaders = lines.first().let {
            val parts = it.split(delimiters)
            listOf("symbol", "ticker").contains(parts[0].toLowerCase())
        }

        //
        // determine if  the 1st line is a header
        //
        val headers = lines
                .first()
                .let {
                    val parts = it.split(delimiters)
                    if (listOf("symbol", "ticker").contains(parts[0].toLowerCase())) {
                        parts.mapIndexed { index, value -> index to value }.toMap()
                    } else {
                        mapOf(0 to "ticker", 1 to "amount")
                    }
                }
        val prunedLines = if (hasHeaders) lines.drop(1) else lines
        val parsedRows = prunedLines.filter { it.isNotBlank() }.mapIndexed { index, line ->
            val parts = line.split(delimiters)
            val part1 = parts[0]
            val part2 = parts[1]
            val asset = resolveAssetId(headers[0], part1)
            if (asset == null) {
                PositionRow(error = ResolvePortfolioError(message = "Unable to find identifier $part1", unresolvedIdentifier = part1, index = index))
            } else {
                if (headers[1] == "weight") {
                    // process weight
                    val weight = part2.toDoubleOrNull()
                    if (weight == null) {
                        PositionRow(error = ResolvePortfolioError(message = "Unable to read weight for $part1", unresolvedIdentifier = part1, index = index))
                    } else {
                        PositionRow(asset = asset, position = Position(quantity = quantity(asset, fixedNav * weight), assetId = asset.id))
                    }
                } else if (listOf("quantity", "amount", "unit").contains(headers[1]?.toLowerCase())) {
                    // process the unit
                    val quantity = part2.toDoubleOrNull()
                    if (quantity == null) {
                        PositionRow(error = ResolvePortfolioError(message = "Unable to read the amount for $part1", index = index))
                    } else {
                        PositionRow(asset = asset, position = Position(quantity = quantity, assetId = asset.id))
                    }
                } else {
                    // assume the amounts are in market value
                    val marketValue = part2.toDoubleOrNull()
                    if (marketValue == null) {
                        PositionRow(error = ResolvePortfolioError(message = "Unable to read the market value for $part1", index = index))
                    } else {
                        PositionRow(asset = asset, position = Position(quantity = quantity(asset, marketValue), assetId = asset.id))
                    }
                }
            }
        }

        val assets = parsedRows.mapNotNull { it.asset?.let { asset -> asset.id to asset } }.toMap()

        return ImportPortfolioResponse(
                positionRows = parsedRows,
                assets = assets,
                requiresNavForScaling = headers[1] == "weight"
        )

    }

    private fun quantity(asset: Asset, marketValue: Double): Double {
        return marketValue / (asset.price ?: 0.0)
    }

    private fun resolveAssetId(symbol: String?, identifier: String): Asset? {
        return try {
            assetService.getAssetByTicker(identifier)
        } catch (e: Exception) {
            null
        }
    }
}
