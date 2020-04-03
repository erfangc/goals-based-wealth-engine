package io.github.erfangc.goalsengine.models

import io.github.erfangc.users.models.WhiteListItem

data class ConstructEfficientFrontierRequest(val whiteList: List<WhiteListItem>)