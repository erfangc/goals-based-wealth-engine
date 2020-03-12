package io.github.erfangc.analysis

import io.github.erfangc.assets.Asset

data class AnalysisResponse(val analysis: Analysis, val assets: Map<String, Asset>)