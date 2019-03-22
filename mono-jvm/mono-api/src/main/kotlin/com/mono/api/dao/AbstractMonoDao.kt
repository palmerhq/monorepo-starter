package com.mono.api.dao

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mongodb.client.MongoDatabase
import io.stardog.stardao.mongodb.AbstractMongoDao
import io.stardog.stardao.mongodb.mapper.jackson.modules.ExtendedJsonModule
import io.stardog.stardao.mongodb.mapper.jackson.modules.MongoModule
import org.bson.Document
import java.time.Instant
import java.util.*

abstract class AbstractMonoDao<M, P, K, I>: AbstractMongoDao<M, P, K, I> {
    companion object {
        val DEFAULT_EXTENDED_JSON_MAPPER = ObjectMapper()
                .registerModule(JavaTimeModule())
                .registerModule(Jdk8Module())
                .registerModule(KotlinModule(10))
                .registerModule(ExtendedJsonModule())

        val DEFAULT_OBJECT_MAPPER = ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .registerModule(Jdk8Module())
                .registerModule(KotlinModule())
                .registerModule(MongoModule())

        val IN = "\$in"
        val SET = "\$set"
        val GT = "\$gt"
        val GTE = "\$gte"
        val LT = "\$lt"
        val LTE = "\$lte"
        val INC = "\$inc"
    }

    constructor(modelClass : Class<M>, partialClass: Class<P>, db: MongoDatabase, collectionName: String) :
        super(modelClass, partialClass, db.getCollection(collectionName), DEFAULT_OBJECT_MAPPER, DEFAULT_EXTENDED_JSON_MAPPER)

    fun loadNullable(id: K): M? {
        return loadOpt(id).orElse(null)
    }

    fun softDelete(id: K, userId: I) {
        val query = Document("_id", id)
        val update = Document(SET, Document("deleteAt", Date()).append("deleteId", userId))
        collection.updateOne(query, update)
    }
}