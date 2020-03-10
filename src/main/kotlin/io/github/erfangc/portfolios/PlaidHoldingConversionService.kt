package io.github.erfangc.portfolios

import com.plaid.client.response.InvestmentsHoldingsGetResponse
import io.github.erfangc.assets.AssetService
import org.springframework.stereotype.Service

@Service
class PlaidHoldingConversionService(private val assetService: AssetService) {
    fun converter(investments: InvestmentsHoldingsGetResponse): PlaidHoldingConverter {
        return PlaidHoldingConverter(assetService, investments)
    }
}

