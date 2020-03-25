package io.github.erfangc.users

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit


object AccessTokenProvider {

    private val log = LoggerFactory.getLogger(AccessTokenProvider::class.java)

    private val key = if (System.getenv("JWT_SIGNING_KEY") != null) {
        log.info("Found JWT signing key in environment variable JWT_SIGNING_KEY")
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(System.getenv("JWT_SIGNING_KEY")))
    } else {
        log.info("Generating a new HMAC-SHA256 signing key to sign JWT")
        Keys.secretKeyFor(SignatureAlgorithm.HS256)
    }

    fun signAccessTokenFor(user: User): String {
        val now = Instant.now()
        val issuer = "www.wealth-engine.com"
        return Jwts
                .builder()
                .setSubject(user.id)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(TimeUnit.MILLISECONDS.convert(36L, TimeUnit.HOURS))))
                .setIssuer(issuer)
                .setAudience(issuer)
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