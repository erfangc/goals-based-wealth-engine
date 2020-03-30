package io.github.erfangc.analysis.models

import io.github.erfangc.assets.models.Asset

data class AnalysisResponse(val analysis: Analysis, val assets: Map<String, Asset>)