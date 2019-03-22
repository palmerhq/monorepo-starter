package com.mono.api.resources

import com.stripe.model.Invoice
import com.stripe.model.InvoiceCollection
import com.stripe.model.Plan
import com.stripe.model.SubscriptionItem
import com.mono.api.dao.CustomerDao
import com.mono.api.data.embed.CardInfo
import com.mono.api.data.embed.StripeInvoice
import com.mono.api.data.entity.Job
import com.mono.api.data.entity.PartialCustomer
import com.mono.api.data.entity.PartialJob
import com.mono.api.data.request.SignupRequest
import com.mono.api.data.request.StripeSourceRequest
import com.mono.api.data.response.SignupResponse
import com.mono.api.services.EmailService
import com.mono.test.customer
import com.mono.test.injector
import com.mono.test.userAuth
import io.stardog.email.emailers.TestEmailer
import io.stardog.email.interfaces.TemplateEmailer
import io.stardog.stardao.jackson.JsonHelper
import io.stardog.starwizard.services.stripe.StripeService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.YearMonth
import java.util.*
import javax.ws.rs.ForbiddenException
import kotlin.test.assertFailsWith

internal class CustomerResourceTest {
    val resource = injector.getInstance(CustomerResource::class.java)

    @Test
    fun signupCustomer() {
        val customerDao = injector.getInstance(CustomerDao::class.java)
        customerDao.dropAndInitTable()

        val emailer = injector.getInstance(TemplateEmailer::class.java) as TestEmailer
        val emailService = injector.getInstance(EmailService::class.java)

        emailer.clear()
        emailService.initEmails()

        val signupReq = SignupRequest(
                userName = "Ian White",
                customerName = "Stardog Ventures",
                email = "example@example.com",
                token = "stripe-token",
                cardInfo = CardInfo("Visa", YearMonth.of(2022, 5), "1234"),
                job = PartialJob(
                        title = "Signup Job",
                        companyName = "Stardog Ventures",
                        description = "Test description",
                        type = Job.Type.FULL,
                        location = "New York, NY",
                        category = Job.Category.DEVELOPMENT,
                        remoteFriendly = true,
                        applyUrl = URI.create("http://example.com/job"),
                        companyUrl = URI.create("http://example.com")),
                jobCount = 1
        )
        val stripeService = injector.getInstance(StripeService::class.java)
        val fakeCustomer = com.stripe.model.Customer()
        fakeCustomer.id = "stripe-customer-id"

        val fakeSubscription = com.stripe.model.Subscription()
        fakeSubscription.id = "stripe-subscription-id"
        val subItem1 = SubscriptionItem()
        subItem1.id = "item-1"
        subItem1.plan = Plan()
        subItem1.plan.id = "plan_base_monthly"
        fakeSubscription.subscriptionItems = com.stripe.model.SubscriptionItemCollection()
        fakeSubscription.subscriptionItems.data = listOf(subItem1)

        Mockito.`when`(stripeService.createCustomer(eq("stripe-token"), eq("Stardog Ventures"), eq("example@example.com"), ArgumentMatchers.any()))
                .thenReturn(fakeCustomer)
        Mockito.`when`(stripeService.createSubscription(eq("stripe-customer-id"), eq(mapOf("JOB" to 1L)), eq(mapOf())))
                .thenReturn(fakeSubscription)

        val signup = resource.signupCustomer(null, signupReq)
        kotlin.test.assertEquals(201, signup.status)
        val response = signup.entity as SignupResponse
        val customerId = response.customer.id

        val load = customerDao.load(customerId)
        kotlin.test.assertEquals("stripe-customer-id", load.stripeCustomerId)
        kotlin.test.assertEquals("stripe-subscription-id", load.stripeSubscriptionId)
        kotlin.test.assertEquals("item-1", load.stripeSubscriptionItems!![0].id)
        assertEquals("1234", load.cardInfo?.last4)

        // verify that the email has been sent
        val lastTemplate = emailer.lastTemplateSend
        kotlin.test.assertEquals("Stardog Ventures", lastTemplate.vars["customerName"])

        // can't create a customer again since the email address is taken
        assertFailsWith<ForbiddenException> {
            resource.signupCustomer(null, signupReq)
        }
    }

    @Test
    fun updateStripeSource() {
        val customerDao = injector.getInstance(CustomerDao::class.java)
        customerDao.dropAndInitTable()

        customerDao.create(PartialCustomer(customer))

        val result = resource.updateStripeSource(userAuth, customer.id, JsonHelper.obj(
                "{source:'test-source',cardInfo:{last4:'1234',brand:'Visa',expire:'2022-05'}}", StripeSourceRequest::class.java))
        assertEquals(204, result.status)

        val load = customerDao.load(customer.id)
        assertEquals("1234", load.cardInfo?.last4)
        assertEquals("Visa", load.cardInfo?.brand)
        assertEquals(YearMonth.of(2022, 5), load.cardInfo?.expire)
    }

    @Test
    fun getCustomerInvoices() {
        val customerDao = injector.getInstance(CustomerDao::class.java)
        customerDao.dropAndInitTable()
        customerDao.create(PartialCustomer(customer.copy(stripeCustomerId = "abc123")))

        val stripeService = injector.getInstance(StripeService::class.java)
        val invoiceCollection = InvoiceCollection()
        val invoice = Invoice()
        invoice.amountDue = 5000
        invoice.amountPaid = 2500
        invoice.currency = "usd"
        invoice.date = 1543788688L
        invoice.hostedInvoiceUrl = "https://example.com/invoice"
        invoiceCollection.data = listOf(invoice)
        Mockito.`when`(stripeService.listInvoices(mapOf("customer" to "abc123", "limit" to null)))
                .thenReturn(invoiceCollection)

        val result = resource.getCustomerInvoices(userAuth, customer.id, null)
        assertEquals(1, result.data.size)
        assertEquals(StripeInvoice(
                amountDue = BigDecimal("50.00"),
                amountPaid = BigDecimal("25.00"),
                currency = Currency.getInstance("USD"),
                createAt = Instant.ofEpochSecond(1543788688L),
                hostedInvoiceUrl = URI.create("https://example.com/invoice")), result.data[0])

    }
}