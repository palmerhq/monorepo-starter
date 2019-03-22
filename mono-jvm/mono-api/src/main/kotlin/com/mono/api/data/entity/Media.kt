package com.mono.api.data.entity

import com.fasterxml.jackson.annotation.JsonInclude
import io.stardog.stardao.annotations.*
import io.stardog.stardao.auto.annotations.DataPartial
import io.swagger.annotations.ApiModelProperty
import org.bson.types.ObjectId
import java.time.Instant

@DataPartial
@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class Media(
        @Id
        @ApiModelProperty("unique id of media", dataType = "string", example = "123456781234567812345678")
        val id: ObjectId,

        @ApiModelProperty("MIME content-category of media")
        val type: String,

        @ApiModelProperty("path to media in storage")
        val path: String,

        @ApiModelProperty("list of versions that are available")
        val versions: Set<String>,

        @ApiModelProperty("width of image")
        val width: Int?,

        @ApiModelProperty("height of image")
        val height: Int?,

        @ApiModelProperty("size of media in bytes")
        val bytes: Long,

        @CreatedAt
        @ApiModelProperty("created timestamp")
        val createAt: Instant,

        @CreatedBy
        @ApiModelProperty("created by user id")
        val createId: ObjectId
)