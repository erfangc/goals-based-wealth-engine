package io.github.erfangc.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import io.github.erfangc.common.DynamoDBUtil.fromItem
import io.github.erfangc.common.DynamoDBUtil.toItem
import io.github.erfangc.users.internal.AccessTokenProvider.signAccessTokenFor
import io.github.erfangc.users.models.*
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes


@Service
class UserService(private val ddb: AmazonDynamoDB) {

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
            val getItemRequest = GetItemRequest("users", mapOf("id" to AttributeValue(email))).withAttributesToGet("password")
            val password = ddb.getItem(getItemRequest).item["password"]?.s
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
        val item = toItem(user)
        ddb.putItem(
                "users",
                (item.entries.map { entry -> entry.key to entry.value }
                        + ("password" to AttributeValue(hashedPassword))).toMap()
        )
        return SignUpResponse(user = user)
    }

}
