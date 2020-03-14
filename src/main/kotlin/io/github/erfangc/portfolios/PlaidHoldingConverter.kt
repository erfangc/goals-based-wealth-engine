package io.github.erfangc.portfolios

import com.plaid.client.response.InvestmentsHoldingsGetResponse
import io.github.erfangc.assets.AssetService
import org.slf4j.LoggerFactory

class PlaidHoldingConverter constructor(
        private val assetService: AssetService,
        investments: InvestmentsHoldingsGetResponse) {

    private val securities = investments.securities.associateBy { it.securityId }
    private val log = LoggerFactory.getLogger(PlaidHoldingConverter::class.java)

    fun convert(holding: InvestmentsHoldingsGetResponse.Holding): Position? {
        val security = securities[holding.securityId] ?: error("")
        val asset = security
                .cusip
                ?.let { cusip -> assetService.getAssetByCUSIP(cusip) }
                ?: security.tickerSymbol?.let { ticker -> assetService.getAssetByTicker(ticker) }
        if (asset == null) {
            log.info(
                    "Unable to resolve asset with securityId=${security.securityId}, " +
                    "name=${security.name}, " +
                    "cusip=${security.cusip}, " +
                    "isin=${security.isin}, " +
                    "tickerSymbol=${security.tickerSymbol}, " +
                    "institutionSecurityId=${security.institutionSecurityId}, " +
                    "type=${security.type}, " +
                    "proxySecurityId=${security.proxySecurityId}"
            )
            return null
        }
        val assetId = asset.id
        val quantity = holding.quantity
        val costBasis = holding.costBasis
        return Position(
                id = assetId,
                assetId = assetId,
                quantity = quantity,
                cost = costBasis
        )
    }

}