package io.github.erfangc.proposals.models

data class Analyses(
        val netAssetValue: NetAssetValue,
        val marketValue: MarketValue,
        val weights: Weights,
        val allocations: GenerateProposalResponseAllocations,
        val expectedReturns: ExpectedReturns,
        val volatility: Volatility,
        val scenarioOutputs: ScenarioOutputs,
        val probabilityOfSuccess: ProbabilityOfSuccess
)
