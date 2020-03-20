package io.github.erfangc.scenarios

data class ScenarioDefinition(
        val id: String,
        val name: String,
        val scenarioShocks: List<ScenarioShock>
)

