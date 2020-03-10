package io.github.erfangc.portfolios

import com.plaid.client.PlaidClient
import com.plaid.client.request.InvestmentsHoldingsGetRequest
import com.plaid.client.request.ItemPublicTokenExchangeRequest
import com.plaid.client.response.InvestmentsHoldingsGetResponse
import io.github.erfangc.assets.AssetService
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.util.*

@Service
class PlaidService(private val plaidClient: PlaidClient,
                   private val portfolioService: PortfolioService,
                   private val assetService: AssetService
) {
    /**
     * Links all investment accounts found via the Plaid
     * public token to the given clientId (client as in an advisor's client not a Plaid API client)
     */
    fun linkItem(clientId: String, publicToken: String): List<Portfolio> {
        // per Plaid flows, exchange an public token for an access token
        val accessToken = plaidClient
                .service()
                .itemPublicTokenExchange(ItemPublicTokenExchangeRequest(publicToken))
                .execute()
                .body()
                .accessToken

        val investmentHoldings = investmentHoldings(publicToken)

        val portfolios = portfolioService
                .getForClientId(clientId)
                ?.filter { it.source?.itemId != null }
                ?.associateBy { it.source?.accountId } ?: emptyMap()

        val item = investmentHoldings.item
        val institutionId = item.institutionId
        val itemId = item.itemId

        val accountHoldings = investmentHoldings.holdings.groupBy { it.accountId }

        // build a look up map of plaid security id to the security itself
        val securities = investmentHoldings.securities.associateBy { it.securityId }

        // for every pair of accountId to holding, create a portfolio (or replace an existing one as a sync)
        val updatedPortfolios = accountHoldings.map { (accountId, holdings) ->
            val portfolio = portfolios[accountId]
            val positions = holdings.map { holding ->
                val security = securities[holding.securityId] ?: error("")
                val cusip = security.cusip
                val asset = assetService.getAssetByCUSIP(cusip) ?: throw RuntimeException("cannot find security $cusip")
                val assetId = asset.assetId
                val quantity = holding.quantity
                val costBasis = holding.costBasis
                Position(
                        id = assetId,
                        assetId = assetId,
                        quantity = quantity,
                        cost = costBasis
                )
            }
            portfolio?.copy(positions = positions, source = portfolio.source?.copy(accessToken = accessToken))
                    ?: Portfolio(
                            id = UUID.randomUUID().toString(),
                            positions = positions,
                            clientId = clientId,
                            source = Source(
                                    institutionId = institutionId,
                                    accessToken = accessToken,
                                    accountId = accountId,
                                    itemId = itemId
                            )
                    )
        }

        updatedPortfolios.forEach {
            portfolio ->
            portfolioService.savePortfolio(portfolio)
        }

        return updatedPortfolios
    }

    private fun investmentHoldings(accessToken: String): InvestmentsHoldingsGetResponse {
        return plaidClient
                .service()
                .investmentsHoldingsGet(InvestmentsHoldingsGetRequest(accessToken))
                .execute()
                .body()
    }
}