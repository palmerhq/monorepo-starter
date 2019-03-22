package com.mono.api.data.embed

import org.bson.types.ObjectId

data class CustomerAccess(
        val customerId: ObjectId,
        val access: Access
) {
    enum class Access(val level: Int) {
        NONE(0), MEMBER(1), ADMIN(2);

        fun isAtLeast(other: Access): Boolean {
            return level >= other.level
        }
    }
}
