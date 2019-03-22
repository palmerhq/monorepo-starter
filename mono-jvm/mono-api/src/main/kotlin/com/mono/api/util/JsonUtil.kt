package com.mono.api.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.stardog.stardao.mongodb.mapper.jackson.modules.MongoModule
import java.util.HashMap

object JsonUtil {
    val MAPPER = ObjectMapper()
            .registerModule(Jdk8Module())
            .registerModule(MongoModule())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)

    fun json(obj: Any?): String {
        try {
            return MAPPER.writeValueAsString(obj)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun <T> obj(json: String, klazz: Class<T>): T {
        try {
            return MAPPER.readValue(json, klazz)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun toMap(obj: Any): Map<String, Any> {
        return MAPPER.convertValue(obj, object : TypeReference<HashMap<String, Any>>() { })
    }
}
