package io.github.erfangc.proposals

import io.github.erfangc.clients.ClientService
import io.github.erfangc.proposals.models.GenerateProposalGivenClientIdRequest
import io.github.erfangc.proposals.models.GenerateProposalRequest
import io.github.erfangc.proposals.models.GenerateProposalResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping
class ProposalsController(private val proposalsService: ProposalsService, private val clientService: ClientService) {
    @PostMapping("/apis/clients/{clientId}/_generate-proposal")
    fun generateProposal(@PathVariable clientId: String,
                         @RequestBody req: GenerateProposalGivenClientIdRequest): GenerateProposalResponse {
        val client = clientService.getClient(clientId)
        return proposalsService.generateProposal(
                GenerateProposalRequest(
                        client = client,
                        newInvestment = req.newInvestment,
                        proposalName = req.proposalName,
                        proposalDescription = req.proposalDescription
                )
        )
    }
}