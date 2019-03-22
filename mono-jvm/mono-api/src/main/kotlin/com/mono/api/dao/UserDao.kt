package com.mono.api.dao

import com.mono.api.data.entity.PartialUser
import com.mono.api.data.entity.User
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.types.ObjectId
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDao @Inject constructor(db: MongoDatabase): AbstractMonoDao<User,PartialUser,ObjectId,ObjectId>
    (User::class.java, PartialUser::class.java, db, "user") {

    fun emailExists(email: String, excludeId: ObjectId?): Boolean {
        return exists(Document("email", email), excludeId)
    }

    fun loadUserByEmail(email: String): User {
        return loadByQuery(Document("email", email), null)
    }

    fun findOneByEmail(email: String): User? {
        return loadByQueryOpt(Document("email", email)).orElse(null)
    }

    fun findOneByGithubId(githubId: String): User? {
        return loadByQueryOpt(Document("githubId", githubId)).orElse(null)
    }

    fun updateLoginAt(id: ObjectId, at: Instant) {
        collection.updateOne(
                Document("_id", id),
                Document(SET, Document("loginAt", Date.from(at))))
    }

    fun updatePassword(id: ObjectId, passwordCrypt: String) {
        collection.updateOne(
                Document("_id", id),
                Document(SET, Document("passwordCrypt", passwordCrypt)))
    }

    fun updateGithubIdName(id: ObjectId, githubId: String, githubName: String) {
        collection.updateOne(
                Document("_id", id),
                Document(SET, Document("githubId", githubId).append("githubName", githubName)))
    }

    fun countByCustomer(customerId: ObjectId): Long {
        val query = Document("customers.customerId", customerId)
        return collection.count(query)
    }

    fun iterateByProject(projectId: ObjectId): Iterable<User> {
        val query = Document("projects.projectId", projectId)
        return iterateByQuery(query, null)
    }
}