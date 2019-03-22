package com.mono.api.access

import com.mono.api.auth.UserAuth
import com.mono.api.data.embed.CustomerAccess
import com.google.common.collect.Sets
import io.stardog.stardao.core.Results
import io.stardog.stardao.core.Update
import org.bson.types.ObjectId

abstract class AbstractAccessControl<M,P> {
    abstract fun toVisible(auth: UserAuth?, model: M): P

    open fun canRead(auth: UserAuth?, model: M): Boolean {
        return true
    }

    open fun canCreate(auth: UserAuth, create: P): Boolean {
        return false
    }

    open fun canUpdate(auth: UserAuth, model: M, update: Update<P>): Boolean {
        return false
    }

    open fun canDelete(auth: UserAuth, model: M): Boolean {
        return false;
    }

    fun <N> toVisibleResults(auth: UserAuth?, iterable: Iterable<M>): Results<P, N> {
        val partialResults = iterable
                .filter { m -> canRead(auth, m) }
                .map { m -> toVisible(auth, m) }
        return Results.of(partialResults)
    }

    fun <N> toVisibleResults(auth: UserAuth?, iterable: Iterable<M>, limit: Int, nextFunction: (partial: P) -> N): Results<P, N> {
        val partialResults = iterable
                .filter { m -> canRead(auth, m) }
                .take(limit + 1)
                .map { m -> toVisible(auth, m) }

        return if (partialResults.size <= limit) {
            Results.of(partialResults)
        } else {
            Results.of(partialResults.subList(0, limit), nextFunction(partialResults[limit]))
        }
    }

    fun getCustomerAccess(auth: UserAuth?, customerId: ObjectId): CustomerAccess.Access {
        if (auth == null) {
            return CustomerAccess.Access.NONE
        }
        return auth.user.getCustomerAccess(customerId)
    }

    fun hasCustomerAccess(auth: UserAuth?, customerId: ObjectId): Boolean {
        return getCustomerAccess(auth, customerId).level > 0
    }

    fun isSuperuser(auth: UserAuth?): Boolean {
        return auth?.user?.isSuperuser() == true
    }

    fun isUpdateAllowed(update: Update<P>, allowedFields: Set<String>): Boolean {
        return Sets.difference(update.updateFields, allowedFields).isEmpty()
    }

}