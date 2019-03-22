package com.mono.api.data.embed

data class StripePlan(
        val productId: String,
        val planId: String,
        val type: Type
) {
    enum class Type { LICENSED }
}