package io.github.erfangc.assets.parser

import org.junit.jupiter.api.Test

internal class YFinanceFundAssetParserTest {

    @Test
    fun forTicker() {
        YFinanceFundAssetParser().parseTicker("BNDX")
        YFinanceFundAssetParser().parseTicker("BOND")
        YFinanceFundAssetParser().parseTicker("PTTRX")
        YFinanceFundAssetParser().parseTicker("MALOX")
        YFinanceFundAssetParser().parseTicker("AGG")
        YFinanceFundAssetParser().parseTicker("EEM")
    }
}