package io.github.erfangc.portfolios

import com.plaid.client.PlaidClient
import com.plaid.client.request.InvestmentsHoldingsGetRequest
import com.plaid.client.request.ItemPublicTokenExchangeRequest
import com.plaid.client.response.InvestmentsHoldingsGetResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PlaidService(private val plaidClient: PlaidClient,
                   private val portfolioService: PortfolioService,
                   private val plaidHoldingConversionService: PlaidHoldingConversionService
) {
    private val log = LoggerFactory.getLogger(PlaidService::class.java)
    /**
     * Links all investment accounts found via the Plaid
     * public token to the given clientId (client as in an advisor's client not a Plaid API client)
     */
    fun linkItem(clientId: String, publicToken: String): LinkItemResponse {
        // per Plaid flows, exchange an public token for an access token
        val accessToken = accessToken(publicToken)
        val investments = investmentHoldings(accessToken)

        val item = investments.item

        // accounts = Plaid linked accounts, portfolios = our internal representation
        val accounts = investments.accounts.associateBy { it.accountId }
        val portfolios = portfolios(clientId)

        // utilities
        val accountHoldings = investments.holdings.groupBy { it.accountId }
        val converter = plaidHoldingConversionService.converter(investments)

        // for every pair of accountId to holding, create a portfolio (or replace an existing one as a sync)
        val updatedPortfolios = accountHoldings
                .map { (accountId, holdings) ->
                    val portfolio = portfolios[accountId]
                    val account = accounts[accountId]
                    log.info("Processing Plaid linked account $accountId, name=${account?.name}")
                    val positions = holdings.mapNotNull { converter.convert(it) }
                    portfolio?.copy(positions = positions, source = portfolio.source?.copy(accessToken = accessToken))
                            ?: Portfolio(
                                    id = UUID.randomUUID().toString(),
                                    positions = positions,
                                    clientId = clientId,
                                    source = Source(
                                            name = account?.name,
                                            mask = account?.mask,
                                            subType = account?.subtype,
                                            type = account?.type,
                                            institutionId = item.institutionId,
                                            accessToken = accessToken,
                                            accountId = account?.accountId!!,
                                            itemId = item.itemId
                                    )
                            )
                }

        updatedPortfolios.forEach { portfolio ->
            portfolioService.savePortfolio(portfolio)
        }

        return LinkItemResponse(updatedPortfolios)
    }

    private fun portfolios(clientId: String): Map<String?, Portfolio> {
        return portfolioService
                .getForClientId(clientId)
                ?.filter { it.source?.itemId != null }
                ?.associateBy { it.source?.accountId } ?: emptyMap()
    }

    private fun accessToken(publicToken: String): String {
        return plaidClient
                .service()
                .itemPublicTokenExchange(ItemPublicTokenExchangeRequest(publicToken))
                .execute()
                .body()
                .accessToken
    }

    private fun investmentHoldings(accessToken: String): InvestmentsHoldingsGetResponse {
        return plaidClient
                .service()
                .investmentsHoldingsGet(InvestmentsHoldingsGetRequest(accessToken))
                .execute()
                .body()
    }
}