package io.github.erfangc.users.settings

import io.github.erfangc.scenarios.ScenarioDefinition

data class Settings(
        val whiteList: List<WhiteListItem> = emptyList(),
        val scenarioDefinitions: List<ScenarioDefinition> = emptyList(),
        val modelPortfolioSettings: ModelPortfolioSettings = ModelPortfolioSettings()
)

