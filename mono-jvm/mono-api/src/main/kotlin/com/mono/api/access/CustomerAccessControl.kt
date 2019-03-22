package com.mono.api.access

import com.mono.api.auth.UserAuth
import com.mono.api.data.embed.CustomerAccess
import com.mono.api.data.entity.Customer
import com.mono.api.data.entity.PartialCustomer
import io.stardog.stardao.core.Update

class CustomerAccessControl: AbstractAccessControl<Customer, PartialCustomer>() {
    override fun toVisible(auth: UserAuth?, model: Customer): PartialCustomer {
        if (auth?.user?.isSuperuser() == true || hasCustomerAccess(auth, model.id)) {
            return PartialCustomer(model)
        }
        return PartialCustomer(
            id = model.id,
            name = model.name,
            status = model.status
        )        
    }

    override fun canRead(auth: UserAuth?, model: Customer): Boolean {
        return hasCustomerAccess(auth, model.id)
    }

    override fun canCreate(auth: UserAuth, create: PartialCustomer): Boolean {
        return auth.user.isSuperuser()
    }

    override fun canUpdate(auth: UserAuth, model: Customer, update: Update<PartialCustomer>): Boolean {
        if (isSuperuser(auth)) {
            return true
        }

        if (!getCustomerAccess(auth, model.id).isAtLeast(CustomerAccess.Access.ADMIN)) {
            return false
        }

        // admins can update name
        return isUpdateAllowed(update, setOf("name"))
    }

    fun canUpdateSource(auth: UserAuth, model: Customer): Boolean {
        if (isSuperuser(auth)) {
            return true
        }

        return getCustomerAccess(auth, model.id).isAtLeast(CustomerAccess.Access.ADMIN)
    }

    fun canReadStripe(auth: UserAuth, model: Customer): Boolean {
        return canUpdateSource(auth, model)
    }

    override fun canDelete(auth: UserAuth, model: Customer): Boolean {
        return isSuperuser(auth)
    }
}