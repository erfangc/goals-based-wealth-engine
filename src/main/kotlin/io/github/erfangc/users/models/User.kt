package io.github.erfangc.users.models

import io.github.erfangc.users.models.Settings

data class User(
        val id: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val settings: Settings = Settings(),
        val clientIds: List<String> = emptyList(),
        val address: String? = null,
        val firmName: String? = null
)