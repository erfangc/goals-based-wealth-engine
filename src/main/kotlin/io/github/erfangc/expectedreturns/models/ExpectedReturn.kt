package io.github.erfangc.expectedreturns.models

data class ExpectedReturn(
        val expectedReturn: Double,
        val marketSensitivity: Double? = null,
        val smb: Double? = null,
        val hml: Double? = null,
        val yield: Double? = null
)