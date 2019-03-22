package com.mono.api.app

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import io.stardog.dropwizard.worker.data.WorkerConfig

class MonoApiConfig(
        val env: String = "local",
        val allowedOrigins: String = "*",
        val webUrl: String = "http://localhost:3000",
        val mongoUri: String = "mongodb://localhost/?connectTimeoutMS=1000&serverSelectionTimeoutMS=1000",
        val mongoDatabase: String = "mono-local",
        val redisHost: String? = null,
        val redisPort: Int = 6379,
        val redisChannel: String = "mono",
        val redisPassword: String? = null,
        val awsRegion: String = "us-east-1",
        val awsProfile: String? = null,
        val s3BucketData: String = "mono-data-local",
        val s3BucketMedia: String = "mono-media-local",
        val s3LocalPath: String? = null,
        val sqsWorkQueueName: String? = null,
        val workerConfig: WorkerConfig? = null,
        val emailService: String = "none",
        val emailWhitelist: Set<String> = setOf(),
        val githubClientId: String? = null,
        val githubClientSecret: String? = null,
        val mailgunPrivateKey: String? = null,
        val mailgunDomain: String? = null,
        val stripeSecretKey: String? = null,
        val stripeSigningSecret: String? = null,
        val enableSlack: Boolean = false,
        val slackUsername: String = "mono",
        val slackChannel: String = "mono",
        val slackIconUrl: String? = null,
        val slackWebhookUrl: String? = null,
        val swagger: SwaggerBundleConfiguration? = null
) : Configuration()