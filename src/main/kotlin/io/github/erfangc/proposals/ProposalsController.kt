package io.github.erfangc.proposals

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/proposals")
class ProposalsController(private val proposalsService: ProposalsService) {
    @PostMapping("_generate-proposal")
    fun generateProposal(@RequestBody req: GenerateProposalRequest): GenerateProposalResponse {
        return proposalsService.generateProposal(req)
    }
}