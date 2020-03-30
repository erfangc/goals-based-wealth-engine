package io.github.erfangc.clients

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.github.erfangc.clients.models.Client
import io.github.erfangc.common.DynamoDBUtil.fromItem
import io.github.erfangc.common.DynamoDBUtil.toItem
import io.github.erfangc.users.UserService
import org.springframework.stereotype.Service

@Service
class ClientService(private val ddb: AmazonDynamoDB, private val userService: UserService) {

    fun getClients(): List<Client> {
        val user = userService.currentUser()
        return user.clientIds.chunked(25).flatMap { chunk ->
            val items = ddb.batchGetItem(
                    mapOf("clients" to
                            KeysAndAttributes().withKeys(chunk.map { clientId -> mapOf("id" to AttributeValue(clientId)) }))
            ).responses["clients"]
            items?.map { item -> fromItem<Client>(item) } ?: emptyList()
        }
    }

    fun getClient(id: String): Client? {
        val user = userService.currentUser()
        val notFoundException = RuntimeException("Unable to find client $id")
        val clientId = user.clientIds.find { it == id } ?: throw notFoundException
        return try {
            fromItem<Client>(ddb.getItem("clients", mapOf("id" to AttributeValue(clientId))).item)
        } catch (e: ResourceNotFoundException) {
            throw notFoundException
        }
    }

    fun saveClient(client: Client): Client {
        val user = userService.currentUser()
        // save the client object itself
        val oldAttributes = ddb.putItem("clients", toItem(client), "ALL_OLD").attributes
        if (oldAttributes == null || !oldAttributes.containsKey("id")) {
            // if this is a new client object, save it to the user object
            userService.saveUser(user.copy(clientIds = user.clientIds + client.id))
        }
        return client
    }

    fun deleteClient(id: String): Client? {
        val user = userService.currentUser()
        ddb.deleteItem("clients", mapOf("id" to AttributeValue(id)), "ALL_OLD")
        userService.saveUser(user.copy(clientIds = user.clientIds.filter { it != id }))
        return getClient(id)
    }
    
}