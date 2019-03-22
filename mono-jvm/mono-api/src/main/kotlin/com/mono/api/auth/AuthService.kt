package com.mono.api.auth

import com.mono.api.dao.TokenDao
import com.mono.api.dao.UserDao
import com.mono.api.data.entity.Token
import com.mono.api.data.entity.User
import com.google.common.base.Joiner
import io.dropwizard.auth.AuthenticationException
import io.dropwizard.auth.Authenticator
import io.stardog.stardao.exceptions.DataNotFoundException
import io.stardog.starwizard.data.response.AccessTokenResponse
import io.stardog.starwizard.exceptions.OauthException
import org.apache.commons.lang3.RandomStringUtils
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.ws.rs.BadRequestException

@Singleton
class AuthService @Inject
constructor(private val userDao: UserDao, private val tokenDao: TokenDao, @Named("env") private val env: String) : Authenticator<String, UserAuth> {
    override fun authenticate(tokenId: String): Optional<UserAuth> {
        try {
            val token = tokenDao.loadNullable(tokenId)
            if (token == null) {
                return Optional.empty()
            }
            val member = userDao.load(token.userId)
            return Optional.of(UserAuth(member, token))

        } catch (e: Exception) {
            throw AuthenticationException("Unable to load user from token $tokenId", e)
        }

    }

    fun issueAccessTokenFromUser(user: User, scopes: Collection<Token.Scope>): AccessTokenResponse {
        val expireSecs: Long = 60 * 60 * 24 * 30          // 30 days
        val token = issueAccessToken(user.id, scopes, expireSecs)
        userDao.updateLoginAt(user.id, Instant.now())
        val scopeString = Joiner.on(" ").join(scopes).toLowerCase()
        return AccessTokenResponse.of(token.id, "bearer", expireSecs.toInt(), null, scopeString)
    }

    fun issueAccessToken(userId: ObjectId, scopes: Collection<Token.Scope>, expireSecs: Long): Token {
        val tokenId = RandomStringUtils.randomAlphanumeric(64)
        return tokenDao.create(Token(
                id = tokenId,
                userId = userId,
                scopes = scopes.toSet(),
                createAt = Instant.now(),
                expireAt = Instant.now().plusSeconds(expireSecs)))
    }
}
