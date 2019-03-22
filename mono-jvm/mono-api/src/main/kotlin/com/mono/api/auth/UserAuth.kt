package com.mono.api.auth

import com.mono.api.data.entity.Token
import com.mono.api.data.entity.User
import org.bson.types.ObjectId
import java.security.Principal

data class UserAuth(
        val user: User,
        val token: Token
): Principal {
    override fun getName(): String {
        return user.email
    }
}