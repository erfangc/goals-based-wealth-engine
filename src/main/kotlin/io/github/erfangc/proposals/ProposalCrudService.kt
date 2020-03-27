package io.github.erfangc.proposals

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.proposals.models.Proposal
import io.github.erfangc.users.UserService
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class ProposalCrudService(private val userService: UserService,
                          private val objectMapper: ObjectMapper,
                          private val jdbcTemplate: NamedParameterJdbcTemplate) {

    private val log = LoggerFactory.getLogger(ProposalCrudService::class.java)

    fun getClient(id: String): Proposal? {
        val userId = userService.currentUser().id
        val row = jdbcTemplate.queryForMap("SELECT * FROM proposals WHERE id = :id AND userId = :userId", mapOf("id" to id, "userId" to userId))
        return objectMapper.readValue(row["json"].toString())
    }

    fun saveProposal(proposal: Proposal): Proposal {
        val userId = userService.currentUser().id
        val clientId = proposal.clientId
        log.info("Saving proposal ${proposal.id} for client ${proposal.clientId}")
        val json = objectMapper.writeValueAsString(proposal)
        val updateSql = """
            INSERT INTO proposals (id, userId, clientId, json)
            VALUES (:id, :userId, :clientId, CAST(:json AS json))
            ON CONFLICT (id, userId)
            DO
            UPDATE
            SET userId = :userId, json = CAST(:json AS json), clientId = :clientId
        """.trimIndent()
        jdbcTemplate.update(
                updateSql,
                mapOf(
                        "id" to proposal.id,
                        "userId" to userId,
                        "json" to json,
                        "clientId" to clientId
                )
        )
        log.info("Saved proposal ${proposal.id} for client ${proposal.clientId}")
        return proposal
    }

    fun deleteProposal(id: String): Proposal? {
        val userId = userService.currentUser().id
        val client = getClient(id)
        jdbcTemplate.update(
                "DELETE FROM proposals WHERE id = :id AND userId = :userId",
                mapOf("id" to id, "userId" to userId)
        )
        return client
    }

}