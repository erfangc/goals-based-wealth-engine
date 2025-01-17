package io.github.erfangc.clients.models

import java.time.LocalDate

data class Client(
        val id: String,
        val goals: Goals? = null,
        val modelPortfolioId: String? = null,
        val automateProposal: Boolean? = null,
        val firstName: String,
        val lastName: String,
        val email: String? = null,
        val birthDay: LocalDate? = null,
        val portfolioIds: List<String> = emptyList(),
        val proposalIds: List<String> = emptyList()
)