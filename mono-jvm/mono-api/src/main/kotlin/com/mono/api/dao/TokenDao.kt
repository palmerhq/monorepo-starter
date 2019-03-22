package com.mono.api.dao

import com.mono.api.data.entity.Token
import com.mongodb.client.MongoDatabase
import org.bson.types.ObjectId
import javax.inject.Inject

class TokenDao @Inject constructor(db: MongoDatabase): AbstractMonoDao<Token, Token, String, ObjectId>
    (Token::class.java, Token::class.java, db, "token") {
}