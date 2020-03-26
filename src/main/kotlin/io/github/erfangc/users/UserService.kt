package io.github.erfangc.users

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.erfangc.users.AccessTokenProvider.signAccessTokenFor
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes


@Service
class UserService(private val jdbcTemplate: NamedParameterJdbcTemplate,
                  private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun currentUser(): User {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        if (requestAttributes is ServletRequestAttributes) {
            val userId = requestAttributes.request.getAttribute("userId")
            return getUser(userId as String)
        } else {
            throw RuntimeException("Unable to determine userId")
        }
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
            throw RuntimeException("We were unable to sign you in")
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
