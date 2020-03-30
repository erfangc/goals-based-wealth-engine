package io.github.erfangc.scenarios

import io.github.erfangc.scenarios.models.ScenarioAnalysisResponse
import io.github.erfangc.scenarios.models.ScenariosAnalysisRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/scenarios-service")
class ScenariosController(private val scenariosService: ScenariosService) {
    @PostMapping("_analyze")
    fun analyze(@RequestBody req: ScenariosAnalysisRequest): ScenarioAnalysisResponse {
        return scenariosService.scenariosAnalysis(req)
    }
}