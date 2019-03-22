package com.mono.api.services

import com.mono.api.auth.AuthService
import com.mono.api.dao.UserDao
import com.mono.api.data.entity.Media
import com.mono.api.data.entity.PartialUser
import com.mono.api.data.entity.User
import com.mono.api.util.JsonUtil
import io.stardog.starwizard.services.http.HttpService
import io.stardog.starwizard.services.media.MediaService
import io.stardog.starwizard.services.media.MediaUtil
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.message.BasicHeader
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.BadRequestException

class GithubAuthService @Inject constructor(
        val httpService: HttpService,
        val mediaService: MediaService,
        val userDao: UserDao,
        @Named("githubClientId") val clientId: String,
        @Named("githubClientSecret") val clientSecret: String
) {
    private val LOG = LoggerFactory.getLogger(GithubAuthService::class.java)

    fun getTokenFromCode(code: String): String {
        val uri = URI.create("https://github.com/login/oauth/access_token")
        val params = mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "code" to code
        )
        val tokenData = httpService.postUrlEncoded(uri, params, listOf(BasicHeader("Accept", "application/json")))
        return tokenData["access_token"]?.toString()
                ?: throw BadRequestException("Did not receive access token in response")
    }

    fun getUserInfo(githubToken: String): Map<String,Any?> {
        val url = URI.create("https://api.github.com/user")
        return httpService.getJson(url, mapOf(), listOf(BasicHeader("Authorization", "Bearer $githubToken")))
    }

    fun getPrimaryEmail(githubToken: String): String? {
        val url = URI.create("https://api.github.com/user/emails")
        val data = httpService.getJson(url, mapOf(), listOf(BasicHeader("Authorization", "Bearer $githubToken")))

        @Suppress("UNCHECKED_CAST")
        val emails = data["data"] as List<Map<String,Any?>>
        val primary = emails.first { it["primary"] == true }
        return primary["email"]?.toString()
    }

    fun getOrCreateUser(userInfo: Map<String,Any?>, email: String?): User {
        println("Checking: " + JsonUtil.json(userInfo))
        val githubId = userInfo["id"]?.toString()
                ?: throw BadRequestException("Did not receive user id in response from Github")

        // first, try to find a user by github id
        var user = userDao.findOneByGithubId(githubId)
        if (user != null) {
            return user
        }

        val githubName = userInfo["login"]?.toString()
                ?: throw BadRequestException("Did not receive login in response from Github")

        // failing that, find a user by email
        user = userDao.findOneByEmail(email ?: throw BadRequestException("Could not determine primary email from Github"))
        if (user != null) {
            // if found a user matching the email, assign the github id if none was previously assigned
            if (user.githubId == null) {
                LOG.info("Assigned github id $githubId to ${user.id}")
                userDao.updateGithubIdName(user.id, githubId, githubName)
            }
            return user
        }

        // if we have no record of this person in the system, create the user on the fly and return it
        val name = userInfo["name"]?.toString()
                ?: throw BadRequestException("Did not receive name in response from Github")
//        val avatarUrl = userInfo["avatar_url"]?.toString() // @todo - download avatar and set it as user image

        val userId = ObjectId()
        LOG.info("Creating user ${userId} from github id ${githubId}")
        return userDao.create(PartialUser(
                id = userId,
                name = name,
                email = email,
                githubId = githubId,
                githubName = githubName,
                status = User.Status.ACTIVE,
                admin = User.AdminAccess.NORMAL,
                customers = setOf()
        ), Instant.now(), userId)
    }
}