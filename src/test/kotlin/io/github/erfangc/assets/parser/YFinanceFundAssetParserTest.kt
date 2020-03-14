package io.github.erfangc.assets.parser

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class YFinanceFundAssetParserTest {

    @Test
    fun forTicker() {
        val ddb = mockk<AmazonDynamoDB>()
        YFinanceFundAssetParser(ddb).parseTicker("BNDX")
        YFinanceFundAssetParser(ddb).parseTicker("BOND")
        YFinanceFundAssetParser(ddb).parseTicker("PTTRX")
        YFinanceFundAssetParser(ddb).parseTicker("MALOX")
        YFinanceFundAssetParser(ddb).parseTicker("AGG")
        YFinanceFundAssetParser(ddb).parseTicker("EEM")
    }
}