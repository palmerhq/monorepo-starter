package com.mono.api.data.response

import com.mono.api.data.entity.Customer
import com.mono.api.data.entity.Job
import com.mono.api.data.entity.User
import io.stardog.starwizard.data.response.AccessTokenResponse

data class SignupResponse(
        val user: User,
        val customer: Customer,
        val job: Job,
        val token: AccessTokenResponse
)