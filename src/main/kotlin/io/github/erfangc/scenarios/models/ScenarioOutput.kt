package io.github.erfangc.scenarios.models

import io.github.erfangc.scenarios.models.ScenarioDefinition

data class ScenarioOutput(
        val id: String,
        val scenarioDefinition: ScenarioDefinition,
        val gainLoss: Double
)