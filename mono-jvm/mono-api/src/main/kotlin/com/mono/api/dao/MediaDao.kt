package com.mono.api.dao

import com.mongodb.client.MongoDatabase
import com.mono.api.data.entity.Media
import org.bson.types.ObjectId
import javax.inject.Inject

class MediaDao @Inject constructor(db: MongoDatabase): AbstractMonoDao<Media, Media, ObjectId, ObjectId>
    (Media::class.java, Media::class.java, db, "media") {
}