package com.mono.api.services

import com.mono.api.data.entity.User
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import io.stardog.email.data.HandlebarsEmailTemplate
import io.stardog.email.emailers.AbstractHandlebarsTemplateEmailer
import io.stardog.email.interfaces.TemplateEmailer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EmailService @Inject constructor(
        val emailer: TemplateEmailer,
        @Named("env") val env: String,
        @Named("webUrl") val webUrl: String) {
    val LOG: Logger = LoggerFactory.getLogger(EmailService::class.java)

    fun sendTemplate(template: String, user: User, vars: Map<String,Any>) {
        emailer.sendTemplate(template, user.email, user.name, vars)
    }

    fun sendTemplate(template: String, email: String, vars: Map<String,Any>) {
        emailer.sendTemplate(template, email, email, vars)
    }

    fun initEmails() {
        emailer.addGlobalVar("WEB_URL", webUrl)
        addTemplate("login", "Login to Mono.com")
        addTemplate("signup", "Welcome To Mono.com")
    }

    fun addTemplate(name: String, subject: String) {
        if (emailer is AbstractHandlebarsTemplateEmailer) {
            val resource = "email/$name.html"
            val resourceStream = javaClass.classLoader.getResourceAsStream(resource)
            val content = CharStreams.toString(InputStreamReader(resourceStream, Charsets.UTF_8))
            val envPrefix = if (env == "prod") "" else "[$env] "
            emailer.addTemplate(HandlebarsEmailTemplate.builder()
                    .name(name)
                    .fromEmail("support@mono.com")
                    .fromName("mono Cloud")
                    .subject(envPrefix + subject)
                    .contentHtml(content)
                    .build())
        }
    }
}