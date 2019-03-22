package com.mono.api.services

import com.mono.api.dao.EventDao
import com.mono.api.data.entity.Event
import org.bson.types.ObjectId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventService @Inject constructor(
        private val eventDao: EventDao
) {
    fun logEvent(type: Event.Type, entityType: Event.EntityType, entityId: ObjectId, userId: ObjectId?): Event {
        return eventDao.create(Event(
                id = ObjectId(),
                type = type,
                entityId = entityId,
                entityType = entityType,
                userId = userId))
    }
}