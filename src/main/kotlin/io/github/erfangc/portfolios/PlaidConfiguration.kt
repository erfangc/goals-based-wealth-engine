package io.github.erfangc.portfolios

import com.plaid.client.PlaidClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlaidConfiguration {

    @Bean
    fun plaidClient(): PlaidClient {
        val plaidClientId = "5e66c0b0237f7400120ae69d"
        val plaidClientSecret = "2ace19ff80a0a394e505c962fa7258"
        val plaidPublicKey = "e62c3a8f5c6fba7b27fd5da71941dc"
        return PlaidClient
                .newBuilder()
                .clientIdAndSecret(plaidClientId, plaidClientSecret)
                .publicKey(plaidPublicKey)
                .sandboxBaseUrl()
                .build()
    }

}