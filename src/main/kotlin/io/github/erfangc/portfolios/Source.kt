package io.github.erfangc.portfolios

data class Source(
        val mask: String? = null,
        val type: String? = null,
        val subType: String? = null,
        val name: String? = null,
        val officialName: String? = null,
        val institutionId: String,
        val itemId: String,
        val accountId: String,
        val accessToken: String
)