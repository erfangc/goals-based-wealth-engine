package io.github.erfangc.scenarios

import io.github.erfangc.portfolios.Portfolio

data class ScenariosAnalysisRequest(
        val portfolios: List<Portfolio>,
        val scenarioDefinitions: List<ScenarioDefinition>
)