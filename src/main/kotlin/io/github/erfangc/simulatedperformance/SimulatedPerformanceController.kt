package io.github.erfangc.simulatedperformance

import io.github.erfangc.simulatedperformance.models.SimulatedPerformanceRequest
import io.github.erfangc.simulatedperformance.models.SimulatedPerformanceResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/simulated-performance-service")
class SimulatedPerformanceController(private val simulatedPerformanceService: SimulatedPerformanceService) {

    @PostMapping("_analyze")
    fun analyze(@RequestBody req: SimulatedPerformanceRequest): SimulatedPerformanceResponse {
        return simulatedPerformanceService.analyze(req)
    }
    
}