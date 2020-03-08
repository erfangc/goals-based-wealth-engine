package io.github.erfangc.proposals

import io.github.erfangc.convexoptimizer.ConvexOptimizerService
import io.github.erfangc.goalsengine.GoalsEngineService
import org.springframework.stereotype.Service

@Service
class ProposalsService(
        private val goalsEngineService: GoalsEngineService,
        private val convexOptimizerService: ConvexOptimizerService
) {

    fun generateProposal(req: GenerateProposalRequest): GenerateProposalResponse {
        req.client
        TODO()
    }

}

