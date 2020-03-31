package io.github.erfangc.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeAction
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import io.github.erfangc.common.DynamoDBUtil.fromItem
import io.github.erfangc.common.DynamoDBUtil.toItem
import io.github.erfangc.users.internal.AccessTokenProvider
import io.github.erfangc.users.models.*
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


@Service
class UserService(private val ddb: AmazonDynamoDB,
                  private val accessTokenProvider: AccessTokenProvider) {

    internal data class PasswordResetTicket(val id: String, val email: String, val createdAt: Instant)

    private val log = LoggerFactory.getLogger(UserService::class.java)
    private val passwordResetTickets = ConcurrentHashMap<String, PasswordResetTicket>()

    fun createResetPasswordTicket(req: CreateResetPasswordTicketRequest): CreateResetPasswordTicketResponse {
        val ticket = PasswordResetTicket(id = UUID.randomUUID().toString(), createdAt = Instant.now(), email = req.email)
        passwordResetTickets[ticket.id] = ticket
        return CreateResetPasswordTicketResponse("/app/reset-password?ticketId=${ticket.id}")
    }

    fun resetPassword(req: ResetPasswordRequest): ResetPasswordResponse {
        val ticket = passwordResetTickets[req.ticketId] ?: throw RuntimeException("Invalid password reset request")
        val expirationTime = ticket.createdAt.plusMillis(TimeUnit.MILLISECONDS.convert( 1, TimeUnit.HOURS))
        if (expirationTime.isBefore(Instant.now())) {
            throw RuntimeException("The request has expired")
        }
        passwordResetTickets.remove(ticket.id)
        val hashedPassword = BCrypt.hashpw(req.newPassword, BCrypt.gensalt())
        ddb.updateItem(
                "users",
                mapOf("id" to AttributeValue(ticket.email)),
                mapOf("password" to AttributeValueUpdate(AttributeValue(hashedPassword), AttributeAction.PUT))
        )
        return ResetPasswordResponse(success = true)
    }

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
        ddb.putItem("users", toItem(user))
        return user
    }

    fun getUser(id: String): User {
        val item = ddb.getItem("users", mapOf("id" to AttributeValue(id))).item
        return fromItem(item)
    }

    fun signIn(req: SignInRequest): SignInResponse {
        val candidate = req.password ?: error("password cannot be blank")
        val email = req.email ?: error("password cannot be blank")
        try {

            val getItemRequest = GetItemRequest(
                    "users",
                    mapOf("id" to AttributeValue(email))
            ).withAttributesToGet("password")

            val password = ddb.getItem(getItemRequest).item["password"]?.s
            if (BCrypt.checkpw(candidate, password)) {
                val user = getUser(req.email)
                return SignInResponse(accessToken = accessTokenProvider.signAccessTokenFor(user))
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
        val item = toItem(user)
        ddb.putItem(
                "users",
                (item.entries.map { entry -> entry.key to entry.value }
                        + ("password" to AttributeValue(hashedPassword))).toMap()
        )
        return SignUpResponse(user = user)
    }

}
