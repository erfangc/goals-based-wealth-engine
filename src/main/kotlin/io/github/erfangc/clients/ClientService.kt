package io.github.erfangc.clients

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.users.UserService
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class ClientService(
        private val jdbcTemplate: NamedParameterJdbcTemplate,
        private val userService: UserService
) {
    private val om = jacksonObjectMapper()

    fun getClient(id: String): Client? {
        val userId = userService.getUser().id
        val row = jdbcTemplate.queryForMap("SELECT * FROM clients WHERE id = :id AND userId = :userId", mapOf("id" to id, "userId" to userId))
        return om.readValue(row["json"].toString())
    }

    fun saveClient(client: Client): Client {
        val userId = userService.getUser().id
        val json = om.writeValueAsString(client)
        val updateSql = """
            INSERT INTO clients (id, userId, json)
            VALUES (:id, :userId, :json)
            ON CONFLICT (id)
            DO
            UPDATE
            SET userId = :userId, json = :json
        """.trimIndent()
        jdbcTemplate.update(updateSql, mapOf("id" to client.id, "userId" to userId, "json" to json))
        return client
    }

    fun deleteClient(id: String): Client? {
        val userId = userService.getUser().id
        val client = getClient(id)
        jdbcTemplate.update(
                "DELETE FROM clients WHERE id = :id userId = :userId",
                mapOf("id" to id, "userId" to userId)
        )
        return client
    }
}