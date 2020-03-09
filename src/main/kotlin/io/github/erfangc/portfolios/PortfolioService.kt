package io.github.erfangc.portfolios

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.users.UserService
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class PortfolioService(private val userService: UserService, private val jdbcTemplate: NamedParameterJdbcTemplate) {

    private val om = jacksonObjectMapper()

    fun getForClientId(clientId: String): List<Portfolio>? {
        val userId = userService.getUser().id
        return jdbcTemplate.queryForList(
                "SELECT * FROM portfolios WHERE userId = :userId AND clientId = :clientId",
                mapOf("clientId" to clientId, "userId" to userId)
        ).map {
            row ->
            om.readValue(row["json"].toString())
        }
    }

    fun getPortfolio(id: String): Portfolio? {
        val userId = userService.getUser().id
        val row = jdbcTemplate.queryForMap("SELECT * FROM portfolios WHERE id = :id AND userId = :userId", mapOf("id" to id, "userId" to userId))
        return om.readValue(row["json"].toString())
    }

    fun savePortfolio(portfolio: Portfolio): Portfolio {
        val userId = userService.getUser().id
        val clientId = portfolio.clientId
        val json = om.writeValueAsString(portfolio)
        val updateSql = """
            INSERT INTO portfolios (id, userId, clientId, json)
            VALUES (:id, :userId, :json, :clientId)
            ON CONFLICT (id)
            DO
            UPDATE
            SET userId = :userId, json = :json, clientId = :clientId
        """.trimIndent()
        jdbcTemplate.update(
                updateSql,
                mapOf(
                        "id" to portfolio.id,
                        "userId" to userId,
                        "json" to json,
                        "clientId" to clientId
                )
        )
        return portfolio
    }

    fun deletePortfolio(id: String): Portfolio? {
        val userId = userService.getUser().id
        val client = getPortfolio(id)
        jdbcTemplate.update(
                "DELETE FROM portfolios WHERE id = :id userId = :userId",
                mapOf("id" to id, "userId" to userId)
        )
        return client
    }

}