package com.mono.api.data.entity

import com.fasterxml.jackson.annotation.JsonInclude
import io.stardog.stardao.annotations.*
import io.stardog.stardao.auto.annotations.DataPartial
import io.swagger.annotations.ApiModelProperty
import org.bson.types.ObjectId
import java.net.URI
import java.time.Instant
import javax.persistence.Id

@DataPartial
@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class Job (
        @Id
        @ApiModelProperty("unique id of job", dataType = "string", example = "123456781234567812345678")
        val id: ObjectId,

        @Creatable
        @ApiModelProperty("customer id job is associated with", dataType = "string", example = "123456781234567812345678")
        val customerId: ObjectId,

        @Updatable
        @ApiModelProperty("status", example = "ACTIVE")
        val status: Job.Status,

        @Updatable
        @ApiModelProperty("category of job", example = "DEVELOPMENT")
        val category: Job.Category,

        @Updatable
        @ApiModelProperty("type of job", example = "INTERNSHIP")
        val type: Job.Type,

        @Updatable
        @ApiModelProperty("title of job", example = "Senior Software Engineer")
        val title: String,

        @Updatable
        @ApiModelProperty("description of job")
        val description: String,

        @Updatable
        @ApiModelProperty("company name", example = "Google")
        val companyName: String,

        @Updatable
        @ApiModelProperty("company website url", example = "https://google.com")
        val companyUrl: URI,

        @ApiModelProperty("number of all time clicks from web", example = "230332")
        val clickCount: Long,

        @Updatable
        @ApiModelProperty("url of external job listing", example = "https://careers.google.com/jobs/123445/")
        val applyUrl: URI,

        @Updatable
        @ApiModelProperty("remote friendly")
        val remoteFriendly: Boolean,

        @Updatable
        @ApiModelProperty("location of job", example = "New York, NY")
        val location: String,

        @Updatable
        @ApiModelProperty("path to media in storage")
        val imagePath: String? = null,

        @CreatedAt
        @ApiModelProperty("created timestamp")
        val createAt: Instant,

        @CreatedBy
        @ApiModelProperty("created by user id")
        val createId: ObjectId,

        @UpdatedAt
        @ApiModelProperty("created timestamp")
        val updateAt: Instant,

        @UpdatedBy
        @ApiModelProperty("updated by user id")
        val updateId: ObjectId,

        @ApiModelProperty("deleted timestamp")
        val deleteAt: Instant? = null,

        @ApiModelProperty("deleted by user id")
        val deleteId: ObjectId? = null

) {
    enum class Status { ACTIVE, INACTIVE, EXPIRED }
    enum class Type { FULL, PART, CONTRACT, INTERNSHIP, FREELANCE }
    enum class Category { DESIGN, DEVELOPMENT }
}