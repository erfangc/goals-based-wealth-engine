package io.github.erfangc.covariance

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/covariance-service")
class CovarianceController(private val covarianceService: CovarianceService) {
    @GetMapping("_compute")
    fun computeCovariances(@RequestParam assetIds: List<String>): ComputeCovariancesResponse {
        return covarianceService.computeCovariances(assetIds)
    }
}