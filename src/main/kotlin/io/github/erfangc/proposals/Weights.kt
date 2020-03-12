package io.github.erfangc.proposals

data class Weights(
        val original: Map<String, Map<String, Double>>,
        val proposed: Map<String, Map<String, Double>>
)