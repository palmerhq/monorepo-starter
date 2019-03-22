package com.mono.api.dao

import com.mono.api.data.entity.Customer
import com.mono.api.data.entity.PartialCustomer
import com.mongodb.client.MongoDatabase
import com.mono.api.data.embed.CardInfo
import com.mono.api.util.JsonUtil
import io.stardog.stardao.jackson.JsonHelper
import org.bson.Document
import org.bson.types.ObjectId
import javax.inject.Inject

class CustomerDao @Inject constructor(db: MongoDatabase): AbstractMonoDao<Customer, PartialCustomer, ObjectId, ObjectId>
    (Customer::class.java, PartialCustomer::class.java, db, "customer") {

    fun iterateAll(from: ObjectId?): Iterable<Customer> {
        val query = Document("deleteAt", null)
        val sort = Document("_id", 1)
        if (from != null) {
            query.put("_id", Document(GTE, from))
        }
        return iterateByQuery(query, sort)
    }

    fun iterateIds(ids: Collection<ObjectId>): Iterable<Customer> {
        val query = Document("_id", Document(IN, ids))
        return iterateByQuery(query, null)
    }

    fun updateCardInfo(id: ObjectId, cardInfo: CardInfo) {
        val query = Document("_id", id)
        val set = Document("cardInfo", JsonUtil.toMap(cardInfo))
        collection.updateOne(query, Document(SET, set))
    }
}