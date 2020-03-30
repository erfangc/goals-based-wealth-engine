package io.github.erfangc.proposals.internal

import io.github.erfangc.proposals.models.ResolveWhiteListItemRequest
import io.github.erfangc.proposals.models.ResolveWhiteListItemResponse
import io.github.erfangc.users.UserService
import io.github.erfangc.users.models.WhiteListItem

/**
 * This service's sole job is to resolve
 * the whitelist for a given portfolio
 *
 * This is an important task as different imported portfolios can have different white lists and not everything
 * on a model portfolio or the advisors' white list is available to all accounts
 */
class WhiteListResolver(private val userService: UserService) {

    /**
     * Retrieves the white list for a given portfolio
     *
     * 401K portfolios will need customized white list depending on the plan sponsor's
     * data input into the system
     *
     * IRA accounts are presumed to be able to trade anything. The default
     * white list that is used to build the efficient frontier is also by default made available
     * as a whitelist to all non-401K accounts. Otherwise, any model portfolio holdings
     * are presumably trade-able and thus on the white list
     *
     */
    fun resolveWhiteListItems(req: ResolveWhiteListItemRequest): ResolveWhiteListItemResponse {
        return when {
            req.portfolio.source?.subType == "401k" -> {
                // for 401K portfolios we either need to know the plan sponsors's white list
                // or we can assume only the current portfolio is possible
                ResolveWhiteListItemResponse(
                        whiteListItems = req.portfolio.positions.map { WhiteListItem(it.assetId) }.distinct()
                )
            }
            req.modelPortfolio != null -> {
                val whiteListItems =
                        (req.portfolio
                                .positions.map { it.assetId } + req.modelPortfolio.portfolio.positions.map { it.assetId })
                                .distinct().map { WhiteListItem(it) }
                ResolveWhiteListItemResponse(whiteListItems = whiteListItems)
            }
            else -> {
                val efficientFrontierWhiteList = userService.currentUser().settings.whiteList.map { it.assetId }
                val whiteListItems =
                        (req.portfolio
                                .positions.map { it.assetId } + efficientFrontierWhiteList)
                                .distinct().map { WhiteListItem(it) }
                ResolveWhiteListItemResponse(whiteListItems = whiteListItems)
            }
        }
    }
}