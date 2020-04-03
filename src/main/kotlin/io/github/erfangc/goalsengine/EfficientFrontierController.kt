package io.github.erfangc.goalsengine

import io.github.erfangc.goalsengine.models.ConstructEfficientFrontierRequest
import io.github.erfangc.goalsengine.models.ConstructEfficientFrontierResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/efficient-frontier-service")
class EfficientFrontierController(private val efficientFrontierService: EfficientFrontierService) {
    @PostMapping("_construct-efficient-frontier")
    fun constructEfficientFrontier(@RequestBody req: ConstructEfficientFrontierRequest): ConstructEfficientFrontierResponse {
        return efficientFrontierService.constructEfficientFrontier(req)
    }
}