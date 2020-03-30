package io.github.erfangc.simulatedperformance.models

import io.github.erfangc.portfolios.models.Portfolio

data class SimulatedPerformanceRequest(val portfolios: List<Portfolio>)