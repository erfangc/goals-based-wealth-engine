package io.github.erfangc.users

data class Overrides(
        val whiteList: List<WhiteListItem>,
        val scenarioDefinitions: List<ScenarioDefinition>
)