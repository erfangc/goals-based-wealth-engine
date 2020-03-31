package io.github.erfangc.portfolios.internal

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import com.plaid.client.PlaidClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlaidConfiguration {

    @Bean
    fun plaidClient(ssm: AWSSimpleSystemsManagement): PlaidClient {
        val request = GetParameterRequest()
                .withName("/wealth-engine/plaid-secret")
                .withWithDecryption(true)
        val result = ssm.getParameter(request)
        val plaidClientId = "5e66c0b0237f7400120ae69d"
        val plaidClientSecret = result.parameter.value
        val plaidPublicKey = "e62c3a8f5c6fba7b27fd5da71941dc"
        return PlaidClient
                .newBuilder()
                .clientIdAndSecret(plaidClientId, plaidClientSecret)
                .publicKey(plaidPublicKey)
                .sandboxBaseUrl()
                .build()
    }

}
