package io.github.erfangc.portfolios

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes
import io.github.erfangc.clients.ClientService
import io.github.erfangc.common.DynamoDBUtil.fromItem
import io.github.erfangc.common.DynamoDBUtil.toItem
import io.github.erfangc.portfolios.models.*
import org.springframework.stereotype.Service

@Service
class PortfolioService(private val ddb: AmazonDynamoDB,
                       private val clientService: ClientService) {

    fun getPortfoliosForClient(clientId: String): GetPortfoliosForClientResponse {
        val portfolios = clientService
                .getClient(clientId)
                ?.portfolioIds
                ?.chunked(25)
                ?.flatMap { chunk ->
                    val keys = chunk.map { portfolioId ->
                        mapOf("id" to AttributeValue(portfolioId))
                    }
                    ddb.batchGetItem(
                            mapOf("portfolios" to KeysAndAttributes().withKeys(keys))).responses["portfolios"]?.map { item ->
                        fromItem<Portfolio>(item)
                    } ?: emptyList()
                } ?: emptyList()
        return GetPortfoliosForClientResponse(portfolios = portfolios)
    }

    fun getPortfolio(clientId: String, portfolioId: String): GetPortfolioResponse {
        val id = resolvePortfolioId(clientId, portfolioId)
        val item = ddb.getItem("portfolios", mapOf("id" to AttributeValue(id))).item
        return GetPortfolioResponse(portfolio = fromItem(item))
    }

    fun savePortfolio(clientId: String, req: SavePortfolioRequest): GetPortfolioResponse {
        val item = toItem(req.portfolio)
        val oldValues = ddb.putItem("portfolios", item, "ALL_OLD").attributes
        if (oldValues == null || oldValues["id"] == null) {
            val client = clientService.getClient(clientId)
            client?.copy(portfolioIds = client.portfolioIds + req.portfolio.id)?.let {
                clientService.saveClient(it)
            }
        }
        return GetPortfolioResponse(req.portfolio)
    }

    fun deletePortfolio(clientId: String, portfolioId: String): DeletePortfolioResponse {
        val oldValues = ddb
                .deleteItem("portfolios", mapOf("id" to AttributeValue(portfolioId)), "ALL_OLD")
                .attributes
        val client = clientService.getClient(clientId) ?: error("")
        clientService.saveClient(client.copy(portfolioIds = client.portfolioIds.filter { it != portfolioId }))
        return DeletePortfolioResponse(portfolio = fromItem(oldValues))
    }

    private fun resolvePortfolioId(clientId: String, portfolioId: String): String {
        return clientService
                .getClient(clientId)
                ?.portfolioIds
                ?.find { it == portfolioId }
                ?: error("Cannot find portfolio $portfolioId for client $clientId")
    }
}