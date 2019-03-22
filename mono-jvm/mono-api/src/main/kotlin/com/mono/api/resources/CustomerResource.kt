package com.mono.api.resources

import com.stripe.model.Invoice
import com.mono.api.access.CustomerAccessControl
import com.mono.api.auth.AuthService
import com.mono.api.auth.UserAuth
import com.mono.api.dao.CustomerDao
import com.mono.api.dao.JobDao
import com.mono.api.services.EmailService
import io.dropwizard.auth.Auth
import com.mono.api.dao.UserDao
import com.mono.api.data.embed.CustomerAccess
import com.mono.api.data.embed.StripeInvoice
import com.mono.api.data.embed.StripePlan
import com.mono.api.data.embed.StripeSubscriptionItem
import com.mono.api.data.entity.*
import com.mono.api.data.request.SignupRequest
import com.mono.api.data.request.StripeSourceRequest
import com.mono.api.data.response.SignupResponse
import com.mono.api.services.UserService
import io.stardog.stardao.core.Results
import io.stardog.stardao.core.Update
import io.stardog.stardao.validation.DefaultValidator
import io.stardog.starwizard.data.response.AccessTokenResponse
import io.stardog.starwizard.services.stripe.StripeService
import io.stardog.starwizard.services.stripe.exceptions.UncheckedStripeException
import io.stardog.starwizard.util.RequestUtil
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.jaxrs.PATCH
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api("customer", description = "Operations relating to customers")
@Path("/v1/customer")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class CustomerResource @Inject constructor(
        val customerDao: CustomerDao,
        val userDao: UserDao,
        val userService: UserService,
        val emailService: EmailService,
        val authService: AuthService,
        val jobDao: JobDao,
        val stripeService: StripeService,
        val accessControl: com.mono.api.access.CustomerAccessControl): AbstractResource() {
    val LOG: Logger = LoggerFactory.getLogger(CustomerResource::class.java)

    @ApiOperation("Return a customer by id")
    @Path("/{customerId}")
    @GET
    fun getCustomer(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @ApiParam("The customer id") @PathParam("customerId") customerId: ObjectId
    ): PartialCustomer {
        val customer = customerDao.load(customerId);
        if (!accessControl.canRead(auth, customer)) {
            throw NotFoundException("Customer not found: $customerId");
        }
        return accessControl.toVisible(auth, customer);
    }

    @ApiOperation("Query the entire list of customers (superuser only)")
    @GET
    fun findAllCustomers(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @ApiParam("Paginate from customer id") @QueryParam("from") from: ObjectId?,
            @ApiParam("Number of results to return") @QueryParam("limit") limit: Int?
    ): Results<PartialCustomer, ObjectId?> {
        if (!auth.user.isSuperuser()) {
            throw ForbiddenException("Permission denied")
        }

        val lim = parseLimit(limit, 10, 100)

        val iterator = customerDao.iterateAll(from)

        return accessControl.toVisibleResults(auth, iterator, lim, { it.id })
    }

    @ApiOperation("Return the list of customers that the current user belongs to")
    @GET
    @Path("/me")
    fun findMyCustomers(
            @ApiParam(hidden = true) @Auth auth: UserAuth
    ): Results<PartialCustomer, ObjectId?> {
        val iterator = customerDao.iterateIds(auth.user.customers.map { it.customerId })

        return accessControl.toVisibleResults(auth, iterator)
    }

    @ApiOperation("Create a customer", response = Customer::class, code = 201)
    @POST
    fun createCustomer(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            create: PartialCustomer
    ): Response {
        val createVal = create.copy(
                status = create.status ?: Customer.Status.ACTIVE
        )
        DefaultValidator.validateCreate(createVal, customerDao)
        if (!accessControl.canCreate(auth, createVal)) {
            throw ForbiddenException("Permission denied")
        }
        val created = customerDao.create(createVal, Instant.now(), auth.user.id)
        return Response.created(URI.create("/v1/customer/" + created.id)).entity(created).build()
    }

    @ApiOperation("Update a customer by id")
    @Path("/{customerId}")
    @PATCH
    fun updateCustomer(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @ApiParam("Customer id") @PathParam("customerId") customerId: ObjectId,
            update: Update<PartialCustomer>
    ): Response {
        DefaultValidator.validateUpdate(update, customerDao)
        val existing = customerDao.load(customerId)
        if (!accessControl.canUpdate(auth, existing, update)) {
            throw ForbiddenException("Permission denied")
        }
        customerDao.update(customerId, update, Instant.now(), ObjectId());
        return Response.noContent().build();
    }

    @ApiOperation("Submit a different credit card source")
    @Path("/{customerId}/source")
    @POST
    fun updateStripeSource(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @ApiParam("Customer id") @PathParam("customerId") customerId: ObjectId,
            request: StripeSourceRequest
    ): Response {
        val existing = customerDao.load(customerId)
        if (!accessControl.canUpdateSource(auth, existing)) {
            throw ForbiddenException("Permission denied")
        }
        try {
            stripeService.updateCustomerSource(existing.stripeCustomerId, request.source)
        } catch (e: UncheckedStripeException) {
            throw ForbiddenException("Failed to update: ${e.message}")
        }
        customerDao.updateCardInfo(customerId, request.cardInfo)
        return Response.noContent().build();
    }

    @ApiOperation("Request a list of recent invoices, directly from Stripe")
    @Path("/{customerId}/invoices")
    @GET
    fun getCustomerInvoices(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @ApiParam("Customer id") @PathParam("customerId") customerId: ObjectId,
            @ApiParam("Limit") @QueryParam("limit") limit: Int?
            ): Results<StripeInvoice,Any> {
        val existing = customerDao.load(customerId)
        if (!accessControl.canReadStripe(auth, existing)) {
            throw ForbiddenException("Permission denied")
        }
        existing.stripeCustomerId ?: throw ForbiddenException("No record on payment processor")

        try {
            val invoices = stripeService.listInvoices(mapOf("customer" to existing.stripeCustomerId, "limit" to limit))
            return Results.of(invoices.data.map { toStripeInvoice(it) })
        } catch (e: UncheckedStripeException) {
            throw ForbiddenException("Could not retrieve invoices from payment processor")
        }
    }

    fun toStripeInvoice(invoice: Invoice): StripeInvoice {
        val amountDue = BigDecimal(invoice.amountDue)
                .setScale(2, RoundingMode.FLOOR)
                .divide(BigDecimal(100), RoundingMode.FLOOR)
        val amountPaid = BigDecimal(invoice.amountPaid)
                .setScale(2, RoundingMode.FLOOR)
                .divide(BigDecimal(100), RoundingMode.FLOOR)
        return StripeInvoice(
                amountDue = amountDue,
                amountPaid = amountPaid,
                currency = Currency.getInstance(invoice.currency.toUpperCase()),
                hostedInvoiceUrl = URI.create(invoice.hostedInvoiceUrl),
                createAt = Instant.ofEpochSecond(invoice.date)
        )
    }

    @ApiOperation("Signup with a valid credit card", response = SignupResponse::class)
    @POST
    @Path("/signup")
    fun signupCustomer(
            @Context request: HttpServletRequest?,
            signup: SignupRequest
    ): Response {
        val customerId = ObjectId()
        val userId = ObjectId()

        if (signup.jobCount < 1) {
            throw BadRequestException("Must purchase at least one job")
        }
        if (signup.jobCount > 100) {
            throw BadRequestException("Too many jobs")
        }

        val createJob = signup.job.copy(customerId = customerId, status = Job.Status.ACTIVE)
        DefaultValidator.validateCreate(createJob, jobDao)
        if (userDao.emailExists(signup.email, null)) {
            throw ForbiddenException("There is already a user with email: ${signup.email}")
        }

        val stripeCustomer: com.stripe.model.Customer
        try {
            stripeCustomer = stripeService.createCustomer(signup.token, signup.customerName, signup.email, mapOf(
                    "customerId" to customerId.toString()
            ))
        } catch (e: UncheckedStripeException) {
            throw ForbiddenException("Credit card charge failed: " + e.message)
        }

        var stripeSubscription: com.stripe.model.Subscription? = null
        var stripeSubItems: List<StripeSubscriptionItem>? = null
        try {
            stripeSubscription = stripeService.createSubscription(stripeCustomer.id, mapOf("JOB" to signup.jobCount), mapOf())
            stripeSubItems = stripeSubscription?.subscriptionItems?.data?.mapIndexed {
                i, it -> StripeSubscriptionItem(it.id, it.plan.id, StripePlan.Type.LICENSED)
            }
        } catch (e: Exception) {
            LOG.error("Unable to create stripeSubscription for new signup for ${stripeCustomer.id}. Continuing with signup, but this may need manual correction!", e)
        }

        val createdUser = userDao.create(PartialUser(
                id = userId,
                name = signup.userName,
                email = signup.email,
                status = User.Status.ACTIVE,
                admin = User.AdminAccess.NORMAL,
                customers = setOf(CustomerAccess(customerId, CustomerAccess.Access.ADMIN))
        ), Instant.now(), userId)

        val createdCustomer = customerDao.create(PartialCustomer(
                id = customerId,
                name = signup.customerName,
                cardInfo = signup.cardInfo,
                stripeCustomerId = stripeCustomer.id,
                stripeSubscriptionId = stripeSubscription?.id,
                stripeSubscriptionItems = stripeSubItems,
                signupIp = RequestUtil.getRemoteIp(request),
                status = Customer.Status.ACTIVE
        ), Instant.now(), userId)

        val tokenExpire: Long = 60 * 60 * 24 * 7
        val createdJob = jobDao.create(createJob, Instant.now(), userId)
        val token = authService.issueAccessToken(userId, setOf(Token.Scope.DEFAULT), tokenExpire)

        emailService.sendTemplate("signup", createdUser, mapOf(
                "customerName" to createdCustomer.name,
                "jobCount" to signup.jobCount
        ))

        return Response.created(URI.create("/v1/customer/" + createdCustomer.id))
                .entity(SignupResponse(
                        user = createdUser,
                        customer = createdCustomer,
                        job = createdJob,
                        token = AccessTokenResponse.of(token.id, "bearer", tokenExpire.toInt())
                ))
                .build()
    }
}