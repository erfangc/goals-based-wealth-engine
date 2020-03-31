package io.github.erfangc.expectedreturns.internal

data class FrenchFamaFactorLevel(
        val id: String = "french-fama-3-factor",
        val date: String,
        val mktMinusRf: Double,
        val smb: Double,
        val hml: Double,
        val rf: Double
)