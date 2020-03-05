package io.github.erfangc.users

data class Overrides(
        val whiteList: List<WhiteListItem>,
        val scenarioDefinitions: List<ScenarioDefinition>,
        val modelPortfolioSettings: ModelPortfolioSettings
)

// BND - Total Bond Market
// BNDX - Total Bond Market International
// VTI - Large Cap
// VEA - Foreign Stocks
// VWO - EM Stocks
// VXF - Mid Cap
