package io.github.erfangc.scenarios.models

import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.scenarios.models.ScenarioDefinition

data class ScenariosAnalysisRequest(
        val portfolios: List<Portfolio>,
        val scenarioDefinitions: List<ScenarioDefinition>
)