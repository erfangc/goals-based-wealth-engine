package io.github.erfangc.users

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.portfolios.Portfolio
import io.github.erfangc.portfolios.Position
import io.github.erfangc.users.AccessTokenProvider.signAccessTokenFor
import io.github.erfangc.users.settings.ModelPortfolio
import io.github.erfangc.users.settings.ModelPortfolioSettings
import io.github.erfangc.users.settings.Settings
import io.github.erfangc.users.settings.WhiteListItem
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class UserService(private val jdbcTemplate: NamedParameterJdbcTemplate,
                  private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun currentUser(): User {
        return User(
                id = "erfangc",
                address = "8710 51St Ave",
                email = "erfangc@gmail.com",
                firmName = "Self Employed",
                firstName = "Erfang",
                lastName = "Chen",
                settings = Settings(
                        whiteList = listOf(
                                WhiteListItem("AGG"),
                                WhiteListItem("BNDX"),
                                WhiteListItem("VTV"),
                                WhiteListItem("VOE"),
                                WhiteListItem("VBR"),
                                WhiteListItem("MUB"),
                                WhiteListItem("EMB")
                        ),
                        modelPortfolioSettings = ModelPortfolioSettings(
                                modelPortfolios = listOf(
                                        ModelPortfolio(
                                                id = "test1",
                                                portfolio = Portfolio(
                                                        id = "test1",
                                                        name = "Ultra conservative model portfolio",
                                                        description = "This portfolio is very conservative and uses mostly" +
                                                                " US bonds with a few international bond ETFs for diversification. The focus" +
                                                                "is capital preservation. In other words, targeting to match return equivalent to inflation",
                                                        positions = listOf(
                                                                Position("AGG", 100.0, "AGG"),
                                                                Position("IVV", 50.0, "IVV"),
                                                                Position("BNDX", 26.0, "BNDX")
                                                        )
                                                ),
                                                labels = listOf("Conservative", "Income focused", "US centric")
                                        )
                                )
                        )
                )
        )
    }

    fun saveUser(user: User): User {
        val json = objectMapper.writeValueAsString(user)
        val updateSql = """
            UPDATE users
            SET
                json = CAST(:json AS json)
            WHERE
                id = :id
        """.trimIndent()
        jdbcTemplate.update(
                updateSql,
                mapOf("id" to user.id, "json" to json)
        )
        return user
    }

    fun getUser(id: String): User {
        val row = jdbcTemplate.queryForMap("select * from users where id = :id", mapOf("id" to id))
        return objectMapper.readValue<User>(row["json"].toString())
    }

    fun signIn(req: SignInRequest): SignInResponse {
        val candidate = req.password?: error("password cannot be blank")
        val email = req.email?: error("password cannot be blank")
        try {
            val row = jdbcTemplate.queryForMap("select * from users where id = :email", mapOf("email" to email))
            val password = row["password"].toString()
            if (BCrypt.checkpw(candidate, password)) {
                val user = getUser(req.email)
                return SignInResponse(accessToken = signAccessTokenFor(user))
            } else {
                throw RuntimeException("The credentials you provided do not match our records")
            }
        } catch (e: Exception) {
            log.error("Unable to sign in the user ${req.email}", e)
            throw java.lang.RuntimeException("We were unable to sign you in")
        }
    }

    fun signUp(req: SignUpRequest): SignUpResponse {
        val hashedPassword = BCrypt.hashpw(req.password, BCrypt.gensalt())
        val email = req.email
        val user = User(
                id = email,
                email = email,
                firstName = req.firstName,
                lastName = req.lastName
        )
        val json = objectMapper.writeValueAsString(user)
        val updateSql = """
            INSERT INTO users (id, password, json)
            VALUES (:id, :password, CAST(:json AS json))
        """.trimIndent()
        jdbcTemplate.update(
                updateSql,
                mapOf("id" to email, "id" to email, "password" to hashedPassword, "json" to json)
        )
        log.info("Signed up user $email")
        return SignUpResponse(user = user)
    }

}
