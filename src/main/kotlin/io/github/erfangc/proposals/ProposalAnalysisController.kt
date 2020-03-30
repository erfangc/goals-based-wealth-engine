package io.github.erfangc.proposals

import io.github.erfangc.proposals.models.AnalyzeProposalRequest
import io.github.erfangc.proposals.models.AnalyzeProposalResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/proposal-analysis-service")
class ProposalAnalysisController(private val proposalAnalysisService: ProposalAnalysisService) {
    @PostMapping("_analyze")
    fun analyze(@RequestBody req: AnalyzeProposalRequest): AnalyzeProposalResponse {
        return proposalAnalysisService.analyze(req)
    }
}