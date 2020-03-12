package io.github.erfangc.proposals

data class Analyses(
        val netAssetValue: NetAssetValue,
        val marketValue: MarketValue,
        val weights: Weights,
        val allocations: GenerateProposalResponseAllocations,
        val expectedReturn: ExpectedReturn,
        val volatility: Volatility,
        val probabilityOfSuccess: ProbabilityOfSuccess
)

