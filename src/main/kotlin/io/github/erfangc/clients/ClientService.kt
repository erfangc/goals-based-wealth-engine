package io.github.erfangc.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.users.UserService
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class ClientService(
        private val jdbcTemplate: NamedParameterJdbcTemplate,
        private val objectMapper: ObjectMapper,
        private val userService: UserService
) {

    fun getClient(id: String): Client? {
        val userId = userService.getUser().id
        try {
            val row = jdbcTemplate.queryForMap("SELECT * FROM clients WHERE id = :id AND userId = :userId", mapOf("id" to id, "userId" to userId))
            return objectMapper.readValue(row["json"].toString())
        } catch (e: EmptyResultDataAccessException) {
            throw RuntimeException("cannot to find client $id", e)
        }
    }

    fun saveClient(client: Client): Client {
        val userId = userService.getUser().id
        val json = objectMapper.writeValueAsString(client)
        val updateSql = """
            INSERT INTO clients (id, userId, json)
            VALUES (:id, :userId, CAST(:json AS json))
            ON CONFLICT (id, userId)
            DO
            UPDATE
            SET userId = :userId, json = CAST(:json as json)
        """.trimIndent()
        jdbcTemplate.update(updateSql, mapOf("id" to client.id, "userId" to userId, "json" to json))
        return client
    }

    fun deleteClient(id: String): Client? {
        val userId = userService.getUser().id
        val client = getClient(id)
        jdbcTemplate.update(
                "DELETE FROM clients WHERE id = :id AND userId = :userId",
                mapOf("id" to id, "userId" to userId)
        )
        return client
    }
}