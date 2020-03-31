package io.github.erfangc.users.internal

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import io.github.erfangc.users.models.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class AccessTokenProvider(ssm: AWSSimpleSystemsManagement) {

    private val request = GetParameterRequest()
            .withName("/wealth-engine/jwt-signing-key")
            .withWithDecryption(true)
    private val jwtSigningKey = ssm.getParameter(request).parameter.value
    private val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSigningKey))

    fun signAccessTokenFor(user: User): String {
        val now = Instant.now()
        return Jwts
                .builder()
                .setSubject(user.id)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(TimeUnit.MILLISECONDS.convert(36L, TimeUnit.HOURS))))
                .setId(UUID.randomUUID().toString())
                .signWith(key)
                .compact()
    }

    fun parseAccessToken(accessToken: String): Jws<Claims> {
        val jws = try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken)
        } catch (e: Exception) {
            throw RuntimeException("Unable to validate the access token")
        }
        if (jws.body.expiration.before(Date.from(Instant.now()))) {
            throw RuntimeException("Access token expired")
        } else {
            return jws
        }
    }
}