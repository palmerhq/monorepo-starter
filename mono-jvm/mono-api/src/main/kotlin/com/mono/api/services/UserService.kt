package com.mono.api.services

import com.mono.api.dao.UserDao
import com.mono.api.data.embed.CustomerAccess
import com.mono.api.data.entity.Event
import com.mono.api.data.entity.PartialUser
import com.mono.api.data.entity.User
import io.stardog.stardao.core.Update
import org.bson.types.ObjectId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserService @Inject constructor(
        private val userDao: UserDao,
        private val eventService: EventService) {

    fun addCustomers(createdUser: User) {
        updateCustomers(createdUser.copy(customers = setOf()), createdUser.customers)
    }

    fun updateCustomers(user: User, customers: Collection<CustomerAccess>?) {
        if (customers == null) {
            return
        }
        var userCustomers = user.customers.toMutableSet()

        for (cust in customers) {
            val prevAccess = user.getCustomerAccess(cust.customerId)
            if (cust.access != prevAccess) {
                userCustomers.removeIf { it.customerId == cust.customerId }
//                if (cust.access != ProjectAccess.Access.NONE) {
//                    userCustomers.add(cust)
//                }

                val eventType: Event.Type;
                if (cust.access == CustomerAccess.Access.NONE) {
                    eventType = Event.Type.ACCESS_REMOVE
                } else if (prevAccess == CustomerAccess.Access.NONE) {
                    eventType = Event.Type.ACCESS_ADD
                } else {
                    eventType = Event.Type.ACCESS_CHANGE
                }
                eventService.logEvent(eventType, Event.EntityType.CUSTOMER, cust.customerId, user.id)
            }
        }

        if (userCustomers != user.customers) {
            val update = Update.of(PartialUser(customers = userCustomers), setOf("customers"))
            userDao.update(user.id, update)
        }
    }

    fun updateCustomerAccess(user: User, access: CustomerAccess) {
        val customers = user.customers.filter { it.customerId != access.customerId }.toMutableList()
        customers.add(access)
        updateCustomers(user, customers)
    }
}