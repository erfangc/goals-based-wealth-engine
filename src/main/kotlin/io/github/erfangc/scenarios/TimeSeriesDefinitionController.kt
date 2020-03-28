package io.github.erfangc.scenarios

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/time-series-definitions")
class TimeSeriesDefinitionController(private val timeSeriesDefinitionService: TimeSeriesDefinitionService) {
    @GetMapping
    fun timeSeriesDefinitions(): List<TimeSeriesDefinition> {
        return timeSeriesDefinitionService.getTimeSeriesDefinitions()
    }
}