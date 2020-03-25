package io.github.erfangc.users.settings

import io.github.erfangc.scenarios.ScenarioDefinition

data class Settings(
        val whiteList: List<WhiteListItem>,
        val scenarioDefinitions: List<ScenarioDefinition>? = null,
        val modelPortfolioSettings: ModelPortfolioSettings? = null
)

