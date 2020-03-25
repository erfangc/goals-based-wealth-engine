package io.github.erfangc.users

data class SignUpRequest(
        val email: String,
        val password: String,
        val firstName: String,
        val lastName: String,
        val address: String? = null,
        val firmName: String? = null
)