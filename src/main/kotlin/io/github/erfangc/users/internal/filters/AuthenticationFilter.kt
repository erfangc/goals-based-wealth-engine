package io.github.erfangc.users.internal.filters

import io.github.erfangc.users.internal.AccessTokenProvider
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(1)
class AuthenticationFilter(private val accessTokenProvider: AccessTokenProvider) : Filter {

    /**
     * Validate JWT tokens on the request
     */
    override fun doFilter(request: ServletRequest,
                          response: ServletResponse,
                          filterChain: FilterChain) {
        if (request is HttpServletRequest && response is HttpServletResponse) {
            if (request.method == "OPTIONS") {
                filterChain.doFilter(request, response)
                return
            }
            val path = request.servletPath
            if (listOf("/apis/users/_sign-in", "/apis/users/_sign-up").contains(path)) {
                // sign in and sign-up are allowed unauthenticated
                filterChain.doFilter(request, response)
            } else {
                authorizeRequest(request, response, filterChain)
            }
        } else {
            filterChain.doFilter(request, response)
        }

    }

    fun authorizeRequest(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        // extract the Authorization Bearer token from the request
        val authorization = request.getHeader("Authorization")
        if (authorization.isNullOrBlank()) {
            respondWith401(response, "Authorization header missing in request")
        } else {
            val parts = authorization.split(" ")
            if (parts.size != 2) {
                respondWith401(response, "Malformed authorization header, please use format 'Authorization: Bearer <accessToken>'")
            } else {
                val accessToken = parts[1]
                try {
                    val jws = accessTokenProvider.parseAccessToken(accessToken)
                    // the user is authenticated
                    request.setAttribute("userId", jws.body.subject)
                    filterChain.doFilter(request, response)
                } catch (e: RuntimeException) {
                    respondWith401(response, e.message)
                }
            }
        }
    }

    private fun respondWith401(response: HttpServletResponse, message: String? = null) {
        response.status = 401
        response.contentType = "application/json"
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.writer.println("""
            {"message":"${message ?: "Unauthorized"}"}
        """.trimIndent())
    }

}