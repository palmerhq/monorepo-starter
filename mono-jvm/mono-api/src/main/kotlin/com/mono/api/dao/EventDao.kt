package com.mono.api.dao

import com.mongodb.client.MongoDatabase
import com.mono.api.data.entity.Customer
import com.mono.api.data.entity.Event
import org.bson.Document
import org.bson.types.ObjectId
import java.time.Instant
import java.util.*
import javax.inject.Inject

class EventDao @Inject constructor(db: MongoDatabase): AbstractMonoDao<Event, Event, ObjectId, ObjectId>
(Event::class.java, Event::class.java, db, "event") {
    fun countMembers(entityType: Event.EntityType, asOf: Instant): Map<ObjectId,Long> {
        val query = Document("_id", Document(LTE, ObjectId(Date.from(asOf))))
                .append("entityType", entityType.toString())
                .append("type", Document(IN, listOf("ACCESS_ADD", "ACCESS_REMOVE")))
        val result = mutableMapOf<ObjectId,Long>()
        for (doc in collection.find(query)) {
            val mod = if (doc.get("type") == "ACCESS_ADD") 1L else -1L
            val id = doc.getObjectId("entityId")
            result.putIfAbsent(id, 0L)
            result.put(id, result.get(id)!! + mod)
        }
        return result.toMap()
    }
}