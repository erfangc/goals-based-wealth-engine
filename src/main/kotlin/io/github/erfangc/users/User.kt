package io.github.erfangc.users

data class User(
        val id: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val overrides: Overrides? = null,
        val address: String? = null,
        val firmName: String? = null
)