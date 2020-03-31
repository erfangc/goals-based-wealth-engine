package io.github.erfangc.users.models

data class ResetPasswordRequest(val newPassword: String, val ticketId: String)