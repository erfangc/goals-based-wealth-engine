package io.github.erfangc.common

object ErrorUtils {
    fun badInput(message: Any, cause: Throwable? = null): Nothing = throw BadInputException(message.toString(), cause)
    fun error(message: Any, cause: Throwable? = null): Nothing = throw InternalServerError(message.toString(), cause)
}
