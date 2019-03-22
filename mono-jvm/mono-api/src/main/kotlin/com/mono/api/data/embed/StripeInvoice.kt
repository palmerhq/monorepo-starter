package com.mono.api.data.embed

import io.swagger.annotations.ApiModelProperty
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.util.*

data class StripeInvoice(
        @ApiModelProperty("amount due, in dollars")
        val amountDue: BigDecimal,

        @ApiModelProperty("amount paid, in dollars")
        val amountPaid: BigDecimal,

        @ApiModelProperty("three-digit ISO currency code", dataType = "string")
        val currency: Currency,

        @ApiModelProperty("URL of the invoice on Stripe", dataType = "string")
        val hostedInvoiceUrl: URI,

        @ApiModelProperty("timestamp of the creation of the invoice")
        val createAt: Instant
)