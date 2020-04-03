package io.github.erfangc.convexoptimizer

import io.github.erfangc.convexoptimizer.models.ConstrainedMeanVarianceOptimizationRequest
import io.github.erfangc.convexoptimizer.models.ConvexOptimizationResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/convex-optimizer")
class ConvexOptimizerController(private val convexOptimizerService: ConvexOptimizerService) {

    @PostMapping("_constrained-mean-variance-optimization")
    fun constrainedMeanVarianceOptimization(@RequestBody req: ConstrainedMeanVarianceOptimizationRequest): ConvexOptimizationResponse {
        return convexOptimizerService.constrainedMeanVarianceOptimization(req)
    }

}