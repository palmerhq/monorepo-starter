package com.mono.api.data.request

import com.mono.api.data.embed.CardInfo
import com.mono.api.data.entity.PartialJob

data class SignupRequest(
        val userName: String,
        val customerName: String,
        val email: String,
        val token: String,
        val cardInfo: CardInfo,
        val jobCount: Long,
        val job: PartialJob
)