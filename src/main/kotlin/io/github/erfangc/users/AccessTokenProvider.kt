package io.github.erfangc.users

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

    private val key = if (System.getenv("JWT_TOKEN_SIGNING_KEY") != null) {
        log.info("Found JWT signing key in environment variable JWT_TOKEN_SIGNING_KEY")
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(System.getenv("JWT_TOKEN_SIGNING_KEY")))
    } else {
        log.info("Generating a new HMAC-SHA256 signing key to sign JWT")
        Keys.secretKeyFor(SignatureAlgorithm.HS256)
    }

    fun signJwtFor(user: User): String {
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

}