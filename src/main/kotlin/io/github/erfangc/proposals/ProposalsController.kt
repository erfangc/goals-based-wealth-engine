package io.github.erfangc.proposals

import io.github.erfangc.clients.ClientService
import io.github.erfangc.proposals.models.GenerateProposalGivenClientIdRequest
import io.github.erfangc.proposals.models.GenerateProposalRequest
import io.github.erfangc.proposals.models.GenerateProposalResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/proposals-service")
class ProposalsController(private val proposalsService: ProposalsService, private val clientService: ClientService) {
    @PostMapping("_generate-proposal")
    fun generateProposal(@RequestBody req: GenerateProposalRequest): GenerateProposalResponse {
        return proposalsService.generateProposal(req)
    }
}