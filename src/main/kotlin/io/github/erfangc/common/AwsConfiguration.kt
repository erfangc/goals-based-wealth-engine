package io.github.erfangc.common

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AwsConfiguration {
    @Bean
    fun ssm(): AWSSimpleSystemsManagement {
        return AWSSimpleSystemsManagementClientBuilder.defaultClient()
    }
} 