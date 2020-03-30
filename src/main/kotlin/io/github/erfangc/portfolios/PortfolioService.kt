package io.github.erfangc.portfolios

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.portfolios.models.Portfolio
import io.github.erfangc.users.UserService
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class PortfolioService(private val userService: UserService,
                       private val jdbcTemplate: NamedParameterJdbcTemplate) {

    private val om = jacksonObjectMapper()

    fun getForClientId(clientId: String): List<Portfolio>? {
        val userId = userService.currentUser().id
        return jdbcTemplate.queryForList(
                "SELECT * FROM portfolios WHERE userId = :userId AND clientId = :clientId",
                mapOf("clientId" to clientId, "userId" to userId)
        ).map { row ->
            om.readValue<Portfolio>(row["json"].toString())
        }
    }

    fun getPortfolio(id: String): Portfolio? {
        val userId = userService.currentUser().id
        val row = jdbcTemplate.queryForMap("SELECT * FROM portfolios WHERE id = :id AND userId = :userId", mapOf("id" to id, "userId" to userId))
        return om.readValue(row["json"].toString())
    }

    fun savePortfolio(portfolio: Portfolio): Portfolio {
        val userId = userService.currentUser().id
        val clientId = portfolio.clientId
        val json = om.writeValueAsString(portfolio)
        val updateSql = """
            INSERT INTO portfolios (id, userId, clientId, json)
            VALUES (:id, :userId, :clientId, CAST(:json AS json))
            ON CONFLICT (id, userId)
            DO
            UPDATE
            SET userId = :userId, json = CAST(:json AS json), clientId = :clientId
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
        val userId = userService.currentUser().id
        val client = getPortfolio(id)
        jdbcTemplate.update(
                "DELETE FROM portfolios WHERE id = :id AND userId = :userId",
                mapOf("id" to id, "userId" to userId)
        )
        return client
    }

}