package io.github.erfangc.proposals

import io.github.erfangc.proposals.models.DeleteProposalResponse
import io.github.erfangc.proposals.models.GetProposalsByClientIdResponse
import io.github.erfangc.proposals.models.SaveProposalRequest
import io.github.erfangc.proposals.models.SaveProposalResponse
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

    @DeleteMapping("{proposalId}")
    fun deleteProposal(@PathVariable clientId: String, @PathVariable proposalId: String): DeleteProposalResponse {
        return proposalCrudService.deleteProposal(clientId, proposalId)
    }
}