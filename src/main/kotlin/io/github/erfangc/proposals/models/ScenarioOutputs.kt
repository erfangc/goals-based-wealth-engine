package io.github.erfangc.proposals.models

import io.github.erfangc.scenarios.ScenarioOutput

data class ScenarioOutputs(
        val original: List<ScenarioOutput>,
        val proposed: List<ScenarioOutput>
)