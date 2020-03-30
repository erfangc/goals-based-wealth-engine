package io.github.erfangc.users.models

import io.github.erfangc.scenarios.models.ScenarioDefinition

data class Settings(
        val whiteList: List<WhiteListItem> = emptyList(),
        val scenarioDefinitions: List<ScenarioDefinition> = emptyList(),
        val modelPortfolioSettings: ModelPortfolioSettings = ModelPortfolioSettings()
)

