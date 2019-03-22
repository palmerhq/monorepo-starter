package com.mono.api.dao

import com.mono.api.data.entity.PartialJob
import com.mongodb.client.MongoDatabase
import com.mono.api.data.entity.Job
import org.bson.Document
import org.bson.types.ObjectId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobDao @Inject constructor(db: MongoDatabase): AbstractMonoDao<Job, PartialJob, ObjectId, ObjectId>
     (Job::class.java, PartialJob::class.java, db, "job") {

     fun countActiveByCustomer(customerId: ObjectId): Long {
          return collection.count(Document("customerId", customerId).append("status", "ACTIVE").append("deleteAt", null))
     }

     fun iterateActive(from: ObjectId?): Iterable<Job> {
          val query = Document("status", "ACTIVE").append("deleteAt", null)
          if (from != null) {
               query.put("_id", Document(LTE, from))
          }
          val sort = Document("_id", -1)
          return iterateByQuery(query, sort)
     }

     fun iterateByCustomer(customerId: ObjectId, from: ObjectId?): Iterable<Job> {
          val query = Document("customerId", customerId)
          if (from != null) {
               query.put("_id", Document(LTE, from))
          }
          val sort = Document("_id", -1)
          return iterateByQuery(query, sort)
     }

     fun incClickCount(jobId: ObjectId) {
          val query = Document("_id", jobId)
          val update = Document(INC, Document("clickCount", 1))
          collection.updateOne(query, update)
     }
}