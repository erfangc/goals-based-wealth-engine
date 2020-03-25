package io.github.erfangc.users

data class SignInRequest(
        val password: String? = null,
        val email: String? = null,
        val apiKey: String? = null
)