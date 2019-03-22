package com.mono.api.data.entity

import com.fasterxml.jackson.annotation.JsonInclude
import com.mono.api.data.embed.CustomerAccess
import io.stardog.stardao.annotations.*
import io.stardog.stardao.auto.annotations.DataPartial
import io.swagger.annotations.ApiModelProperty
import org.bson.types.ObjectId
import java.time.Instant

@DataPartial
@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class User(
        @Id
        @ApiModelProperty("globally unique id of user", dataType = "string", example = "123456781234567812345678")
        val id: ObjectId,

        @Updatable
        @ApiModelProperty("full name of user", example = "Ian White")
        val name: String,

        @Updatable
        @ApiModelProperty("unique email of user", example = "example@example.com")
        val email: String,

        @Updatable
        @ApiModelProperty("unique github id number of user", example = "12345")
        val githubId: String? = null,

        @Updatable
        @ApiModelProperty("github login name of user", example = "eonwhite")
        val githubName: String? = null,

        @Updatable
        @ApiModelProperty("status")
        val status: Status,

        @Updatable
        @ApiModelProperty("set of customers that this user has access to (should always include any customers covered by projects)")
        val customers: Set<CustomerAccess>,

        @Updatable
        @ApiModelProperty("global admin (superuser) access")
        val admin: AdminAccess,

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
        val updateId: ObjectId
) {
    enum class AdminAccess { NORMAL, SUPERUSER }

    enum class Status { ACTIVE, INACTIVE }

    fun isSuperuser(): Boolean {
            return admin == AdminAccess.SUPERUSER;
    }

    fun getCustomerAccess(customerId: ObjectId): CustomerAccess.Access {
        val customer = customers.firstOrNull( { it.customerId == customerId })
        return if (customer != null) customer.access else CustomerAccess.Access.NONE
    }
}
