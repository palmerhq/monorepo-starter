package com.mono.test

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.fakemongo.Fongo
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.mongodb.client.MongoDatabase
import com.mono.api.senders.TestSender
import io.stardog.dropwizard.worker.interfaces.Sender
import io.stardog.email.emailers.TestEmailer
import io.stardog.email.interfaces.TemplateEmailer
import io.stardog.stardao.jackson.JsonHelper
import io.stardog.starwizard.services.http.HttpService
import io.stardog.starwizard.services.media.MediaService
import io.stardog.starwizard.services.slack.SlackService
import io.stardog.starwizard.services.stripe.StripeService
import org.mockito.Mockito

class TestModule: AbstractModule() {
    override fun configure() {
        JsonHelper.MAPPER.registerModule(KotlinModule())        // needed for Kotlin fake json

        val mongo = Fongo("fake-mongo")
        val mongoDb = mongo.getDatabase("fake-mongo-database")
        bind(MongoDatabase::class.java).toInstance(mongoDb)

        bind(String::class.java)
                .annotatedWith(Names.named("env"))
                .toInstance("test")
        bind(String::class.java)
                .annotatedWith(Names.named("webUrl"))
                .toInstance("http://localhost:3000")
        bind(String::class.java)
                .annotatedWith(Names.named("slackIconUrl"))
                .toInstance("http://example.com/icon")
        bind(String::class.java)
                .annotatedWith(Names.named("slackUsername"))
                .toInstance("formik")
        bind(String::class.java)
                .annotatedWith(Names.named("githubClientId"))
                .toInstance("12345")
        bind(String::class.java)
                .annotatedWith(Names.named("githubClientSecret"))
                .toInstance("1234567890")
        bind(MediaService::class.java).toInstance(Mockito.mock(MediaService::class.java))
        bind(HttpService::class.java).toInstance(Mockito.mock(HttpService::class.java))
        bind(Sender::class.java).toInstance(TestSender())
        bind(Sender::class.java).annotatedWith(Names.named("invalidateSender")).toInstance(TestSender())

        bind(StripeService::class.java)
                .toInstance(Mockito.mock(StripeService::class.java))
        bind(SlackService::class.java)
                .toInstance(Mockito.mock(SlackService::class.java))

        bind(TemplateEmailer::class.java).toInstance(TestEmailer())
    }
}