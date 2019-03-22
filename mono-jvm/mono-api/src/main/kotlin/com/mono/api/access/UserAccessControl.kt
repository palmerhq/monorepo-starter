package com.mono.api.access

import com.google.common.collect.Sets
import com.mono.api.auth.UserAuth
import com.mono.api.data.embed.CustomerAccess
import com.mono.api.data.entity.PartialUser
import com.mono.api.data.entity.User
import io.stardog.stardao.core.Update

class UserAccessControl : com.mono.api.access.AbstractAccessControl<User, PartialUser>() {
    override fun toVisible(auth: UserAuth?, model: User): PartialUser {
        if (isSuperuser(auth) || model.id == auth?.user?.id) {
            return PartialUser(
                    id = model.id,
                    name = model.name,
                    email = model.email,
                    status = model.status,
                    customers = model.customers,
                    admin = model.admin,
                    createAt = model.createAt,
                    createId = model.createId,
                    updateAt = model.updateAt,
                    updateId = model.updateId
            )
        } else {
            return PartialUser(
                    id = model.id,
                    name = model.name
            )
        }
    }

    override fun canRead(auth: UserAuth?, model: User): Boolean {
        return true
    }

    override fun canCreate(auth: UserAuth, create: PartialUser): Boolean {
        return auth.user.isSuperuser()
    }

    override fun canUpdate(auth: UserAuth, model: User, update: Update<PartialUser>): Boolean {
        if (auth.user.isSuperuser()) {
            return true
        }
        if (auth.user.id == model.id) {
            // only allow users to change their name and email
            return Sets.difference(update.updateFields, setOf("name", "email")).isEmpty()
        }

        // for updating other users, allowed to update projects and customers access levels

        if (update.partial.customers != null) {
            for (c in update.partial.customers!!) {
                if (!getCustomerAccess(auth, c.customerId).isAtLeast(CustomerAccess.Access.ADMIN)) {
                    return false
                }
            }
        }

        // if changing customers, verify that the user has customer admin access
//        return Sets.difference(update.updateFields, setOf("projects", "customers")).isEmpty()
        return false
    }
}