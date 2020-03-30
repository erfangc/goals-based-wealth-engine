package io.github.erfangc.users

import io.github.erfangc.users.settings.Settings

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