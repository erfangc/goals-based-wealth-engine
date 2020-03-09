package io.github.erfangc.proposals

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/proposals")
class ProposalsController(private val proposalsService: ProposalsService) {
    @PostMapping("_generate-proposal")
    fun generateProposal(@RequestBody req: GenerateProposalRequest): GenerateProposalResponse {
        return proposalsService.generateProposal(req)
    }

    @GetMapping("{id}")
    fun getProposal(@PathVariable id: String): Proposal {
        TODO()
    }
}