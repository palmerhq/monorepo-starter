package com.mono.api.data.request

data class PasswordChangeRequest(
        val oldPassword: String? = null,
        val newPassword: String
)