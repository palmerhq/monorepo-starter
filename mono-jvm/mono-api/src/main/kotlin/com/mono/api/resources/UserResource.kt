package com.mono.api.resources

import com.mono.api.access.UserAccessControl
import com.mono.api.auth.AuthService
import com.mono.api.auth.UserAuth
import com.mono.api.dao.UserDao
import com.mono.api.data.entity.PartialUser
import com.mono.api.data.entity.Token
import com.mono.api.data.entity.User
import com.mono.api.data.request.EmailRequest
import com.mono.api.data.request.PasswordChangeRequest
import com.mono.api.services.EmailService
import io.dropwizard.auth.Auth
import com.mono.api.services.UserService
import io.stardog.stardao.core.Update
import io.stardog.stardao.exceptions.DataNotFoundException
import io.stardog.stardao.validation.DefaultValidator
import io.swagger.annotations.*
import io.swagger.jaxrs.PATCH
import org.bson.types.ObjectId
import java.net.URI
import java.time.Instant
import javax.inject.Inject
import javax.validation.Valid
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api("user", description = "Operations relating to users")
@Path("/v1/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class UserResource @Inject constructor(
        val userDao: UserDao,
        val authService: AuthService,
        val userService: UserService,
        val emailService: EmailService,
        val accessControl: com.mono.api.access.UserAccessControl) {
    @ApiOperation("Return the current user")
    @Path("/me")
    @GET
    fun getUserMe(
            @ApiParam(hidden = true) @Auth auth: UserAuth
    ): PartialUser {
        val user = userDao.load(auth.user.id);
        return accessControl.toVisible(auth, user);
    }

    @ApiOperation("Return a user by id")
    @Path("/{userId}")
    @GET
    fun getUser(
            @ApiParam(hidden = true) @Auth auth: UserAuth?,
            @ApiParam("The user id") @PathParam("userId") userId: ObjectId
    ): PartialUser {
        val user = userDao.load(userId);
        if (!accessControl.canRead(auth, user)) {
            throw NotFoundException("User not found: $userId");
        }
        return accessControl.toVisible(auth, user);
    }

    @ApiOperation("Create a user", code = 201, response = User::class)
    @POST
    fun createUser(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            create: PartialUser
    ): Response {
        val createVal = create.copy(
                customers = create.customers ?: setOf(),
                admin = create.admin ?: User.AdminAccess.NORMAL,
                status = create.status ?: User.Status.ACTIVE
        )
        DefaultValidator.validateCreate(createVal, userDao)
        if (!accessControl.canCreate(auth, createVal)) {
            throw ForbiddenException("Permission denied")
        }
        val created = userDao.create(createVal, Instant.now(), auth.user.id)
        userService.addCustomers(created)
        return Response.created(URI.create("/v1/user/" + created.id)).entity(created).build()
    }

    @ApiOperation("Update a user by id")
    @Path("/{userId}")
    @PATCH
    fun updateUser(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @ApiParam("User id") @PathParam("userId") userId: ObjectId,
            update: Update<PartialUser>
    ): Response {
        DefaultValidator.validateUpdate(update, userDao)
        val existing = userDao.load(userId)
        if (!accessControl.canUpdate(auth, existing, update)) {
            throw ForbiddenException("Permission denied")
        }
        userDao.update(userId, update, Instant.now(), ObjectId());
        // log project/customer access changes

        userService.updateCustomers(existing, update.partial.customers)

        return Response.noContent().build();
    }

    @Path("/sendlogin")
    @POST
    @ApiOperation(value = "Send an email with a magic login link", code = 204)
    fun sendResetEmail(
            @ApiParam(value = "User email address") @Valid request: EmailRequest): Response {
        try {
            val user = userDao.loadUserByEmail(request.email)
            val token = authService.issueAccessToken(user.id, setOf(Token.Scope.DEFAULT), 60 * 60 * 24 * 7)
            val emailVars = mapOf("token" to token.id)

            emailService.sendTemplate("login", user, emailVars)

        } catch (e: DataNotFoundException) {
            // silently accept if user is not found (to prevent attackers testing for valid emails)
        }

        return Response.noContent().build()
    }
}