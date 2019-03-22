package com.mono.api.module

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClient
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoDatabase
import com.mono.api.app.MonoApiConfig
import io.dropwizard.setup.Environment
import com.mono.api.senders.LocalSender
import com.mono.api.util.JsonUtil
import io.stardog.aws.s3.AmazonS3Local
import io.stardog.dropwizard.worker.interfaces.Sender
import io.stardog.dropwizard.worker.senders.RedisSender
import io.stardog.dropwizard.worker.senders.SqsSender
import io.stardog.email.emailers.MailgunEmailer
import io.stardog.email.emailers.NonEmailer
import io.stardog.email.emailers.SmtpEmailer
import io.stardog.email.interfaces.TemplateEmailer
import io.stardog.starwizard.services.common.AsyncService
import io.stardog.starwizard.services.http.HttpService
import io.stardog.starwizard.services.media.MediaService
import io.stardog.starwizard.services.media.data.ImageVersion
import io.stardog.starwizard.services.media.processors.AsyncImageProcessor
import io.stardog.starwizard.services.media.resizers.ImagemagickResizer
import io.stardog.starwizard.services.media.storage.S3MediaStorage
import net.sargue.mailgun.Configuration
import org.apache.commons.lang3.StringUtils
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.Protocol
import java.io.File
import java.lang.IllegalArgumentException
import java.util.concurrent.Executors

class ApiModule(val config: MonoApiConfig, val env: Environment): AbstractModule() {
    val LOG: Logger = LoggerFactory.getLogger(ApiModule::class.java)

    override fun configure() {
        // configure aws profile if in use
        if (config.awsProfile != null) {
            System.setProperty("aws.profile", config.awsProfile)
        }

        // set up mongodb
        val mongoDatabaseName = StringUtils.left(config.mongoDatabase, 30)
        val mongoClient = MongoClient(MongoClientURI(config.mongoUri))
        val mongoDatabase = mongoClient.getDatabase(mongoDatabaseName)

        val scrubbedMongoUri = config.mongoUri.replace(Regex(":[^@]+@"), ":redacted@")
        LOG.info("Connecting to ${mongoDatabaseName} at $scrubbedMongoUri")

        // set up redis
        if (config.redisHost != null) {
            val jedisPool = JedisPool(GenericObjectPoolConfig(), config.redisHost, config.redisPort, Protocol.DEFAULT_TIMEOUT, config.redisPassword)
            bind(JedisPool::class.java).toInstance(jedisPool)
            bind(Sender::class.java).annotatedWith(Names.named("invalidateSender"))
                    .toInstance(RedisSender(jedisPool, config.redisChannel))
            LOG.info("Connecting to Redis at ${config.redisHost}:${config.redisPort} on channel ${config.redisChannel}" + (if (config.redisPassword != null) " (using AUTH)" else ""))
        } else {
            LOG.info("Redis is not enabled; cache invalidation will not be distributed")
            bind(Sender::class.java).annotatedWith(Names.named("invalidateSender"))
                    .toInstance(LocalSender())
        }

        bind(MongoClient::class.java).toInstance(mongoClient)
        bind(MongoDatabase::class.java).toInstance(mongoDatabase)
        bind(String::class.java).annotatedWith(Names.named("env")).toInstance(config.env)
        bind(String::class.java).annotatedWith(Names.named("webUrl")).toInstance(config.webUrl)
        bind(String::class.java).annotatedWith(Names.named("stripeSecretKey")).toInstance(config.stripeSecretKey)
        bind(String::class.java).annotatedWith(Names.named("stripeSigningSecret")).toInstance(config.stripeSigningSecret)
        bind(String::class.java).annotatedWith(Names.named("githubClientId")).toInstance(config.githubClientId)
        bind(String::class.java).annotatedWith(Names.named("githubClientSecret")).toInstance(config.githubClientSecret)

        // set up AsyncService
        val asyncService = AsyncService(Executors.newFixedThreadPool(10))
        bind(AsyncService::class.java).toInstance(asyncService)
        bind(HttpService::class.java).toInstance(HttpService())

        // configure S3 and media storage
        bind(String::class.java).annotatedWith(Names.named("s3BucketData")).toInstance(config.s3BucketData)
        bind(String::class.java).annotatedWith(Names.named("s3BucketMedia")).toInstance(config.s3BucketMedia)
        val s3: AmazonS3;
        if (config.s3LocalPath != null) {
            s3 = AmazonS3Local(toFilePath(config.s3LocalPath))
        } else {
            s3 = AmazonS3Client.builder()
                    .withRegion(config.awsRegion)
                    .build()
        }
        val mediaStorage = S3MediaStorage(s3, config.s3BucketData, config.s3BucketMedia)
        val imageProcessor = AsyncImageProcessor(asyncService, ImagemagickResizer(), mediaStorage)
        bind(AmazonS3::class.java).toInstance(s3)
        bind(MediaService::class.java).toInstance(
                MediaService(mediaStorage,
                        imageProcessor,
                        setOf("jpg", "gif", "png"),
                        setOf(ImageVersion.of("50x50sq", 50, 50, true))))

        // configure worker queue processing
        if (config.sqsWorkQueueName != null) {
            LOG.info("Using SQS queue: ${config.sqsWorkQueueName}")
            val sqs = AmazonSQSClient.builder().withRegion(config.awsRegion).build()
            bind(AmazonSQS::class.java).toInstance(sqs)
            bind(Sender::class.java).toInstance(SqsSender(sqs, config.sqsWorkQueueName, null, JsonUtil.MAPPER))
        } else {
            LOG.info("Not using live queue, will execute queued messages instantly")
            bind(Sender::class.java).toInstance(LocalSender())
        }

        // set up SlackService
        bind(Boolean::class.java).annotatedWith(Names.named("enableSlack"))
                .toInstance(config.enableSlack)
        bind(String::class.java).annotatedWith(Names.named("slackIconUrl"))
                .toInstance(config.slackIconUrl)
        bind(String::class.java).annotatedWith(Names.named("slackUsername"))
                .toInstance(config.slackUsername)
        bind(String::class.java).annotatedWith(Names.named("slackChannel"))
                .toInstance(config.slackChannel)
        bind(String::class.java).annotatedWith(Names.named("slackWebhookUrl"))
                .toInstance(config.slackWebhookUrl)
        bind(Boolean::class.java).annotatedWith(Names.named("slackInternalOnly"))
                .toInstance(false)
        bind(String::class.java).annotatedWith(Names.named("envPrefix"))
                .toInstance("")

        // set up email
        val emailer: TemplateEmailer
        if (config.emailService == "mailgun") {
            config.mailgunPrivateKey
                    ?: throw IllegalArgumentException("Cannot initialize mailgun email - missing mailgunPrivateKey")
            config.mailgunDomain
                    ?: throw IllegalArgumentException("Cannot initialize mailgun email - missing mailgunDomain")
            val mailgunConfig = Configuration()
                    .apiKey(config.mailgunPrivateKey)
                    .domain(config.mailgunDomain)
            emailer = MailgunEmailer(mailgunConfig)
            LOG.info("Configured to send live emails via Mailgun domain " + config.mailgunDomain)
        } else if (config.emailService == "mailcatcher") {
            LOG.info("Sending emails to MailCatcher on port 1025")
            emailer = SmtpEmailer("localhost", 1025)
        } else {
            emailer = NonEmailer(true)
            LOG.info("Using NonEmailer -- will not actually send live emails")
        }
        if (!config.emailWhitelist.isEmpty()) {
            emailer.setWhitelist(config.emailWhitelist)
            LOG.info("Using email whitelist: " + config.emailWhitelist)
        }
        bind(TemplateEmailer::class.java).toInstance(emailer)
    }

    fun toFilePath(path: String): File {
        return File(path.replace("~", System.getProperty("user.home")))
    }
}