package io.github.erfangc.assets

data class GicsAllocation(
        val basicMaterials: Double = 0.0,
        val consumerCyclical: Double = 0.0,
        val financialServices: Double = 0.0,
        val realEstate: Double = 0.0,
        val consumerDefensive: Double = 0.0,
        val healthCare: Double = 0.0,
        val utilities: Double = 0.0,
        val communicationServices: Double = 0.0,
        val energy: Double = 0.0,
        val industrials: Double = 0.0,
        val technology: Double = 0.0
)