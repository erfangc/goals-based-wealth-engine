package io.github.erfangc.users

data class Settings(
        val whiteList: List<WhiteListItem>,
        val scenarioDefinitions: List<ScenarioDefinition>? = null,
        val modelPortfolioSettings: ModelPortfolioSettings? = null
)

