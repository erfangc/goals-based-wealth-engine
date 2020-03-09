package io.github.erfangc.proposals

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.users.UserService
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class ProposalCrudService(private val userService: UserService, private val jdbcTemplate: NamedParameterJdbcTemplate) {

    private val log = LoggerFactory.getLogger(ProposalCrudService::class.java)
    private val om = jacksonObjectMapper()

    fun getClient(id: String): Proposal? {
        val userId = userService.getUser().id
        val row = jdbcTemplate.queryForMap("SELECT * FROM proposals WHERE id = :id AND userId = :userId", mapOf("id" to id, "userId" to userId))
        return om.readValue(row["json"].toString())
    }

    fun saveProposal(proposal: Proposal): Proposal {
        val userId = userService.getUser().id
        val clientId = proposal.clientId
        log.info("Saving proposal ${proposal.id} for client ${proposal.clientId}")
        val json = om.writeValueAsString(proposal)
        val updateSql = """
            INSERT INTO proposals (id, userId, clientId, json)
            VALUES (:id, :userId, :json, :clientId)
            ON CONFLICT (id)
            DO
            UPDATE
            SET userId = :userId, json = :json, clientId = :clientId
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
        val userId = userService.getUser().id
        val client = getClient(id)
        jdbcTemplate.update(
                "DELETE FROM proposals WHERE id = :id userId = :userId",
                mapOf("id" to id, "userId" to userId)
        )
        return client
    }

}