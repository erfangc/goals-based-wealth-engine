package io.github.erfangc.analysis

import io.github.erfangc.analysis.models.AnalysisRequest
import io.github.erfangc.analysis.models.AnalysisResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/analysis-service")
class AnalysisController(private val analysisService: AnalysisService) {
    @PostMapping("_analyze")
    fun analyze(@RequestBody req: AnalysisRequest): AnalysisResponse {
        return analysisService.analyze(req)
    }
}