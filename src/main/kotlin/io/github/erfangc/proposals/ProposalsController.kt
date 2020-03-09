package io.github.erfangc.proposals

import io.github.erfangc.clients.ClientService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping
class ProposalsController(private val proposalsService: ProposalsService, private val clientService: ClientService) {
    @PostMapping("/apis/clients/{clientId}/_generate-proposal")
    fun generateProposal(@PathVariable clientId: String, @RequestBody req: GenerateProposalGivenClientIdRequest): GenerateProposalResponse {
        val client = clientService.getClient(clientId) ?: throw RuntimeException("cannot find client $clientId")
        return proposalsService.generateProposal(GenerateProposalRequest(client, req.newInvestment, req.save))
    }

    @PostMapping("/apis/proposals/_generate-proposal")
    fun generateProposal(@RequestBody req: GenerateProposalRequest): GenerateProposalResponse {
        return proposalsService.generateProposal(req)
    }

    @GetMapping("/apis/proposals/{id}")
    fun getProposal(@PathVariable id: String): Proposal {
        TODO()
    }
}