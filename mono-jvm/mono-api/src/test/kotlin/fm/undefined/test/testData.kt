package com.mono.test

import com.google.inject.Guice
import com.mongodb.client.MongoDatabase
import com.mono.api.auth.UserAuth
import com.mono.api.data.embed.CustomerAccess
import com.mono.api.data.entity.Customer
import com.mono.api.data.entity.Job
import com.mono.api.data.entity.Token
import com.mono.api.data.entity.User
import org.bson.types.ObjectId
import java.net.URI
import java.time.Instant

val injector = Guice.createInjector(TestModule())
val db = injector.getInstance(MongoDatabase::class.java)
val customerId = ObjectId()
val userId = ObjectId()
val user = User(
        id = userId,
        name = "Test User",
        email = "example@example.com",
        customers = setOf(CustomerAccess(customerId, CustomerAccess.Access.ADMIN)),
        admin = User.AdminAccess.NORMAL,
        status = User.Status.ACTIVE,
        createAt = Instant.now(),
        updateAt = Instant.now(),
        createId = userId,
        updateId = userId
)
val userAuth = UserAuth(user, Token("token", userId, setOf(Token.Scope.DEFAULT), Instant.now(), Instant.now().plusSeconds(60 * 60 * 24)))

val customer = Customer(
        id = customerId,
        name = "Test Customer",
        status = Customer.Status.ACTIVE,
        maxJobs = 5,
        createAt = Instant.now(),
        updateAt = Instant.now(),
        createId = userId,
        updateId = userId
)

val job = Job(
        id = ObjectId(),
        customerId = customerId,
        title = "Test Job",
        companyName = "TestCo",
        description = "This is a test job!",
        location = "New York, NY",
        category = Job.Category.DEVELOPMENT,
        type = Job.Type.FULL,
        createAt = Instant.now(),
        updateAt = Instant.now(),
        createId = userId,
        updateId = userId,
        clickCount = 0,
        remoteFriendly = true,
        status = Job.Status.ACTIVE,
        applyUrl = URI.create("http://example.com/job/1234"),
        companyUrl = URI.create("http://example.com"),
        imagePath = "path/to/image.jpg"
)