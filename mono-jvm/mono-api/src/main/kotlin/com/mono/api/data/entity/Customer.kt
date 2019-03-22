package com.mono.api.data.entity

import com.fasterxml.jackson.annotation.JsonInclude
import com.mono.api.data.embed.CardInfo
import com.mono.api.data.embed.StripeSubscriptionItem
import io.stardog.stardao.annotations.*
import io.stardog.stardao.auto.annotations.DataPartial
import io.swagger.annotations.ApiModelProperty
import org.bson.types.ObjectId
import java.time.Instant

@DataPartial
@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class Customer(
        @Id
        @ApiModelProperty("unique id of customer", dataType = "string", example = "123456781234567812345678", required = true)
        val id: ObjectId,

        @Updatable
        @ApiModelProperty("customer name", required = true)
        val name: String,

        @ApiModelProperty("stripe customer id")
        val stripeCustomerId: String? = null,

        @ApiModelProperty("saved credit card information")
        val cardInfo: CardInfo? = null,

        @ApiModelProperty("stripe subscription id (could be null if on a free plan)")
        val stripeSubscriptionId: String? = null,

        @ApiModelProperty("stripe subscription items (could be null if on a free plan)")
        val stripeSubscriptionItems: List<StripeSubscriptionItem>? = null,

        @Updatable
        @ApiModelProperty("status", required = true)
        val status: Status,

        @ApiModelProperty("maximum number of jobs purchased", required = true)
        val maxJobs: Int,

        @ApiModelProperty("ip address that was used to sign up (null if the customer was created not from the signup page)")
        val signupIp: String? = null,

        @CreatedAt
        @ApiModelProperty("created timestamp")
        val createAt: Instant,

        @CreatedBy
        @ApiModelProperty("created by user id")
        val createId: ObjectId,

        @UpdatedAt
        @ApiModelProperty("updated timestamp")
        val updateAt: Instant,

        @UpdatedBy
        @ApiModelProperty("updated by user id")
        val updateId: ObjectId,

        @ApiModelProperty("deleted timestamp")
        val deleteAt: Instant? = null,

        @ApiModelProperty("deleted by user id")
        val deleteId: ObjectId? = null
) {
        enum class Status { ACTIVE, INACTIVE }
}