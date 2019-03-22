package com.mono.api.data.embed

data class StripeSubscriptionItem(
        val id: String,
        val planId: String,
        val type: StripePlan.Type
)