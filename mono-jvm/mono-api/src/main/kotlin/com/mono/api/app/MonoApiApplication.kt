package com.mono.api.app

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQS
import com.mono.api.auth.AuthService
import com.mono.api.auth.UserAuth
import com.mono.api.exceptionmappers.IllegalArgumentExceptionMapper
import com.mono.api.module.ApiModule
import com.mono.api.services.EmailService
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.reflect.ClassPath
import com.google.inject.Guice
import com.google.inject.Key
import com.google.inject.name.Names
import es.moki.ratelimij.dropwizard.RateLimitBundle
import es.moki.ratelimitj.inmemory.InMemoryRateLimiterFactory
import io.dropwizard.Application
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.federecio.dropwizard.swagger.SwaggerBundle
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import com.mono.api.filters.Json429Filter
import com.mono.api.resources.*
import com.mono.api.senders.LocalSender
import com.mono.api.services.DaoService
import com.mono.api.tasks.CleanupTestDbTask
import com.mono.api.tasks.InitDbTask
import io.stardog.aws.resources.S3ServeResource
import io.stardog.dropwizard.worker.WorkMethods
import io.stardog.dropwizard.worker.WorkTask
import io.stardog.dropwizard.worker.WorkerManager
import io.stardog.dropwizard.worker.data.WorkMethod
import io.stardog.dropwizard.worker.interfaces.Sender
import io.stardog.dropwizard.worker.workers.RedisWorker
import io.stardog.dropwizard.worker.workers.SqsWorker
import io.stardog.stardao.jersey.exceptionmappers.DataNotFoundExceptionMapper
import io.stardog.stardao.jersey.exceptionmappers.DataValidationExceptionMapper
import io.stardog.stardao.mongodb.mapper.jackson.modules.MongoModule
import io.stardog.starwizard.filters.LbHttpsRedirectFilter
import io.stardog.starwizard.handlers.JsonUnauthorizedHandler
import io.stardog.stardao.swagger.PartialConverter
import io.stardog.stardao.swagger.ResultsConverter
import io.stardog.stardao.swagger.UpdateConverter
import io.stardog.starwizard.exceptionmappers.OauthExceptionMapper
import io.stardog.starwizard.mongodb.health.MongoHealthCheck
import io.stardog.starwizard.mongodb.swagger.ObjectIdConverter
import io.stardog.starwizard.params.JavaTimeParamConverterProvider
import io.stardog.starwizard.swagger.AuthParamFilter
import io.swagger.config.FilterFactory
import io.swagger.converter.ModelConverters
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.glassfish.jersey.logging.LoggingFeature
import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import java.util.*
import java.util.logging.Level
import javax.servlet.DispatcherType

class MonoApiApplication : Application<MonoApiConfig>() {
    private val LOG = LoggerFactory.getLogger(MonoApiApplication::class.java)

    override fun initialize(bootstrap: Bootstrap<MonoApiConfig>) {
        super.initialize(bootstrap)
        bootstrap.addBundle(object : SwaggerBundle<MonoApiConfig>() {
            override fun getSwaggerBundleConfiguration(config: MonoApiConfig): SwaggerBundleConfiguration {
                return config.swagger!!
            }
        })

        val rateLimiterFactory = InMemoryRateLimiterFactory()
        bootstrap.addBundle(RateLimitBundle(rateLimiterFactory));

        // support environment variables in config yml
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider, EnvironmentVariableSubstitutor(true))
    }

    override fun run(config: MonoApiConfig, env: Environment) {
        val injector = Guice.createInjector(ApiModule(config, env))

        // configure jackson
        env.objectMapper.registerModule(KotlinModule())
        env.objectMapper.registerModule(JavaTimeModule())
        env.objectMapper.registerModule(Jdk8Module())
        env.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        env.objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
        env.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        env.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
        env.objectMapper.registerModule(MongoModule())

        // resources
        env.jersey().register(injector.getInstance(StatusResource::class.java))
        env.jersey().register(injector.getInstance(UserResource::class.java))
        env.jersey().register(injector.getInstance(OauthResource::class.java))
        env.jersey().register(injector.getInstance(CustomerResource::class.java))
        env.jersey().register(injector.getInstance(MediaResource::class.java))
        env.jersey().register(injector.getInstance(JobResource::class.java))
        env.jersey().register(injector.getInstance(RedirectResource::class.java))
        if (config.s3LocalPath != null) {
            env.jersey().register(S3ServeResource(injector.getInstance(AmazonS3::class.java), config.s3BucketMedia))
        }

        // add auth
        val authenticator = injector.getInstance(AuthService::class.java)
        env.jersey().register(AuthDynamicFeature(
                OAuthCredentialAuthFilter.Builder<UserAuth>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .setUnauthorizedHandler(JsonUnauthorizedHandler())
                        .buildAuthFilter()))
        env.jersey().register(AuthValueFactoryProvider.Binder<UserAuth>(UserAuth::class.java))

        // health checks
        env.healthChecks().register("mongo", injector.getInstance(MongoHealthCheck::class.java))

        // define work methods
        val workMethods = WorkMethods.of(listOf(
                WorkMethod.of("init-db", injector.getInstance(InitDbTask::class.java)),
                WorkMethod.of("cleanup-test-db", injector.getInstance(CleanupTestDbTask::class.java))
        ))
        env.admin().addTask(WorkTask(workMethods))

        // set up SQS if using SQS, otherwise populate the LocalSender
        if (config.sqsWorkQueueName != null) {
            val workerManager = WorkerManager(
                    "worker",
                    config.workerConfig,
                    SqsWorker(workMethods, injector.getInstance(AmazonSQS::class.java), config.sqsWorkQueueName),
                    env.metrics()
            )
            env.lifecycle().manage(workerManager)
        } else {
            val localSender = injector.getInstance(Sender::class.java) as LocalSender
            localSender.workMethods = workMethods
        }

        // set up Redis for pubsub
        if (config.redisHost != null) {
            env.lifecycle().manage(RedisWorker(
                    workMethods, injector.getInstance(JedisPool::class.java), config.redisChannel))
        } else {
            val localSender = injector.getInstance(Key.get(Sender::class.java, Names.named("invalidateSender"))) as LocalSender
            localSender.workMethods = workMethods
        }

        // exception mappers
        env.jersey().register(IllegalArgumentExceptionMapper())
        env.jersey().register(DataNotFoundExceptionMapper())
        env.jersey().register(DataValidationExceptionMapper())
        env.jersey().register(OauthExceptionMapper())

        // allow cross domain requests
        val cors = env.servlets().addFilter("CORS", CrossOriginFilter::class.java)
        cors.setInitParameter("allowedOrigins", config.allowedOrigins)
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin,Authorization")
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,PATCH,HEAD")
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")

        // set up email
        injector.getInstance(EmailService::class.java).initEmails()

        // allow media uploads
        env.jersey().register(MultiPartFeature::class.java)

        // starwizard - allow use of LocalDate and Instant in parameters
        env.jersey().register(JavaTimeParamConverterProvider())

        // starwizard - redirect HTTP to HTTPS
        if (config.env != "local") {
            env.applicationContext.addFilter(FilterHolder(LbHttpsRedirectFilter()), "/*",
                    EnumSet.of(DispatcherType.REQUEST))
        }
        env.jersey().register(Json429Filter())

        // starwizard - allow internal swagger parameters
        FilterFactory.setFilter(AuthParamFilter())

        // swagger - convert ObjectIds properly
        ModelConverters.getInstance().addConverter(ObjectIdConverter())
        ModelConverters.getInstance().addConverter(PartialConverter())
        ModelConverters.getInstance().addConverter(UpdateConverter())
        ModelConverters.getInstance().addConverter(ResultsConverter())

        // swagger - skip partials and updates
        val classPath = ClassPath.from(Thread.currentThread().contextClassLoader)
        for (info in classPath.getTopLevelClasses("com.mono.api.data.entity")) {
            val partialName = info.name.replace(".data.entity.", ".data.entity.Partial")
            ModelConverters.getInstance().addClassToSkip(partialName)
        }
        ModelConverters.getInstance().addClassToSkip("io.stardog.stardao.core.Update")

        // allow full request/response logging
        env.jersey().register(LoggingFeature(java.util.logging.Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME),
                Level.FINE, LoggingFeature.Verbosity.PAYLOAD_ANY, 100000));

        // register all daos via classpath scan, then init, and in non-prod mode, clean up any old test dbs
        injector.getInstance(DaoService::class.java).addDaos(injector)
        workMethods.getMethod("init-db").consumer.accept(mapOf())
        if (config.env != "prod") {
            workMethods.getMethod("cleanup-test-db").consumer.accept(mapOf())
        }
    }
}

fun main(args: Array<String>) {
    MonoApiApplication().run(*args)
}
