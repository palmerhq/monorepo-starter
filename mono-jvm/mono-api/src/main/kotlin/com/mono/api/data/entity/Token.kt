package com.mono.api.data.entity

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModelProperty
import org.bson.types.ObjectId
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class Token(
        @ApiModelProperty("the token identifier")
        val id: String,

        @ApiModelProperty("the user id that the token represents")
        val userId: ObjectId,

        @ApiModelProperty("set of scopes that are issued to this token")
        val scopes: Set<Scope>,

        @ApiModelProperty("the date the token was created")
        val createAt: Instant,

        @ApiModelProperty("the date at which the token is no longer valid")
        val expireAt: Instant
) {
        enum class Scope { DEFAULT }
}

