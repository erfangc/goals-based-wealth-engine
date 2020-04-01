package io.github.erfangc.proposals

import io.github.erfangc.proposals.models.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/clients/{clientId}/proposals")
class ProposalCrudController(private val proposalCrudService: ProposalCrudService) {
    @PostMapping
    fun saveProposal(@PathVariable clientId: String, @RequestBody req: SaveProposalRequest): SaveProposalResponse {
        return proposalCrudService.saveProposal(clientId, req)
    }

    @GetMapping
    fun getProposalsByClientId(@PathVariable clientId: String): GetProposalsByClientIdResponse {
        return proposalCrudService.getProposalsByClientId(clientId)
    }

    @GetMapping("{proposalId}")
    fun getProposal(@PathVariable clientId: String, @PathVariable proposalId: String): GetProposalResponse {
        return proposalCrudService.getProposal(clientId, proposalId)
    }

    @DeleteMapping("{proposalId}")
    fun deleteProposal(@PathVariable clientId: String, @PathVariable proposalId: String): DeleteProposalResponse {
        return proposalCrudService.deleteProposal(clientId, proposalId)
    }
}