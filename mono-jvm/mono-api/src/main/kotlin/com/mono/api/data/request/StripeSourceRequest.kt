package com.mono.api.data.request

import com.mono.api.data.embed.CardInfo
import io.swagger.annotations.ApiModelProperty

data class StripeSourceRequest(
        val source: String,

        val cardInfo: CardInfo
)