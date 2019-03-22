package com.mono.api.data.entity

import com.fasterxml.jackson.annotation.JsonInclude
import io.stardog.stardao.annotations.Id
import io.swagger.annotations.ApiModelProperty
import org.bson.types.ObjectId

@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class Event(
    @Id
    @ApiModelProperty("unique id of event", dataType = "string", example = "123456781234567812345678")
    val id: ObjectId,

    @ApiModelProperty("user who performed the event", dataType = "string", example = "123456781234567812345678")
    val userId: ObjectId?,

    @ApiModelProperty("category of event that happened")
    val type: Type,

    @ApiModelProperty("identity of entity")
    val entityId: ObjectId,

    @ApiModelProperty("category of entity")
    val entityType: EntityType
) {
    enum class EntityType { USER, CUSTOMER, MEDIA, JOB }
    enum class Type { CREATE, UPDATE, DELETE, ACCESS_ADD, ACCESS_REMOVE, ACCESS_CHANGE }
}