package io.github.erfangc.common

class InternalServerError(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)