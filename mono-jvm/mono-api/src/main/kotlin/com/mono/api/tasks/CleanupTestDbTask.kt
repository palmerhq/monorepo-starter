package com.mono.api.tasks

import com.mongodb.MongoClient
import io.stardog.starwizard.services.slack.SlackService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.ws.rs.BadRequestException

@Singleton
class CleanupTestDbTask @Inject constructor(
        @Named("env") val env: String,
        val mongoClient: MongoClient,
        val slackService: SlackService): Consumer<Map<String, Any?>> {
    private val LOG: Logger = LoggerFactory.getLogger(CleanupTestDbTask::class.java)

    override fun accept(params: Map<String, Any?>) {
        if (env == "prod") {
            throw BadRequestException("Cannot call cleanup-test-db in prod")
        }

        val cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS)

        for (dbName in mongoClient.listDatabaseNames()) {
            if (dbName.startsWith("mono-") && dbName != "mono-prod") {
                val db = mongoClient.getDatabase(dbName)
                val meta = db.getCollection("meta").find().first()
                val createAt = meta?.getDate("createAt")?.toInstant()
                if (createAt?.isBefore(cutoffDate) == true) {
                    LOG.info("Dropping old database ${dbName} because created ${createAt}")
                    db.drop()

                    slackService.send("Dropped old test database `${dbName}` (created ${createAt})")
                }
            }
        }
    }
}