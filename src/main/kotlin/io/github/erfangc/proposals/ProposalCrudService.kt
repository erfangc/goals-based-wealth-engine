package io.github.erfangc.proposals

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*
import io.github.erfangc.clients.ClientService
import io.github.erfangc.common.DynamoDBUtil.fromItem
import io.github.erfangc.common.DynamoDBUtil.toItem
import io.github.erfangc.proposals.models.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ProposalCrudService(private val ddb: AmazonDynamoDB,
                          private val clientService: ClientService) {

    fun saveProposal(clientId: String, req: SaveProposalRequest): SaveProposalResponse {
        val proposal = req.proposal.copy(clientId = clientId, updatedAt = Instant.now().toString())
        val item = toItem(proposal)
        val request = PutItemRequest("proposals", item).withReturnValues(ReturnValue.ALL_OLD)
        val result = ddb.putItem(request)
        if (result.attributes == null || result.attributes["id"] == null) {
            val client = clientService.getClient(clientId)
            clientService.saveClient(client.copy(proposalIds = client.proposalIds + proposal.id))
        }
        return SaveProposalResponse(proposal)
    }

    fun getProposal(clientId: String, proposalId: String): GetProposalResponse {
        val item = ddb.getItem("proposals", mapOf("id" to AttributeValue(proposalId))).item
        return GetProposalResponse(fromItem(item))
    }

    fun getProposalsByClientId(clientId: String): GetProposalsByClientIdResponse {
        val proposals = clientService
                .getClient(clientId)
                .proposalIds
                .chunked(25)
                .flatMap { chunk ->
                    val keys = chunk.map { id -> mapOf("id" to AttributeValue(id)) }
                    val batchGetItemRequest = BatchGetItemRequest(mapOf("proposals" to KeysAndAttributes().withKeys(keys)))
                    val items = ddb.batchGetItem(batchGetItemRequest).responses["proposals"] ?: emptyList()
                    items.map { fromItem<Proposal>(it) }
                }
        return GetProposalsByClientIdResponse(proposals)
    }

    fun deleteProposal(clientId: String, proposalId: String): DeleteProposalResponse {
        val deleteItemResult = ddb.deleteItem("proposals", mapOf("id" to AttributeValue(proposalId)), "ALL_OLD")
        val proposal = fromItem<Proposal>(deleteItemResult.attributes)
        return DeleteProposalResponse(proposal)
    }
}