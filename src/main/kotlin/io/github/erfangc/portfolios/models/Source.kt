package io.github.erfangc.portfolios.models

data class Source(
        val mask: String? = null,
        val type: String? = null,
        val subType: String? = null,
        val name: String? = null,
        val officialName: String? = null,
        val institutionId: String,
        val institutionName: String? = null,
        val institutionPrimaryColor: String? = null,
        val itemId: String,
        val accountId: String,
        val accessToken: String
)