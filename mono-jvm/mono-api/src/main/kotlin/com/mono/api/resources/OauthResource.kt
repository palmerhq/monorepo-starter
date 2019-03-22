package com.mono.api.resources

import com.mono.api.auth.AuthService
import com.mono.api.auth.UserAuth
import com.mono.api.dao.TokenDao
import com.mono.api.data.entity.Token
import io.dropwizard.auth.Auth
import com.mono.api.services.GithubAuthService
import io.stardog.starwizard.data.response.AccessTokenResponse
import io.stardog.starwizard.exceptions.OauthException
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory

import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/oauth")
@Api(value = "oauth", description = "Operations relating to Oauth2 authentication")
@Produces(MediaType.APPLICATION_JSON)
class OauthResource @Inject
constructor(
        private val authService: AuthService,
        private val tokenDao: TokenDao,
        private val githubService: GithubAuthService
) {
    private val LOGGER = LoggerFactory.getLogger(OauthResource::class.java)

    @Path("/token/github")
    @POST
    @ApiOperation(value = "Return an Oauth2 Authorization bearer token, given an Oauth authorization code from Github")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun issueAccessTokenFromGithubCode(
            @ApiParam("Github access token") @FormParam("code") code: String,
            @ApiParam(value = "Requested access scope or scopes (space separated)") @FormParam("scope") scopeString: String?): AccessTokenResponse {
        val scopes = parseScope(scopeString)
        val ghToken = githubService.getTokenFromCode(code)
        val ghUser = githubService.getUserInfo(ghToken)
        val ghEmail = githubService.getPrimaryEmail(ghToken)
        val user = githubService.getOrCreateUser(ghUser, ghEmail)

        return authService.issueAccessTokenFromUser(user, scopes)
    }

    @Path("/token")
    @DELETE
    @ApiOperation(value = "Delete the current Oauth2 bearer token (for signout)", code = 204)
    fun deleteToken(
            @ApiParam(hidden = true) @Auth auth: UserAuth): Response {
        tokenDao.delete(auth.token.id)
        return Response.noContent().build()
    }

    fun parseScope(scope: String?): List<Token.Scope> {
        if (scope == null) {
            return listOf(Token.Scope.DEFAULT)
        } else {
            try {
                val scopes = scope.split(" ")
                        .map { name -> Token.Scope.valueOf(name.replace(':', '_').toUpperCase()) }
                return scopes
            } catch (e: IllegalArgumentException) {
                throw OauthException("invalid_scope", "Scope not recognized")
            }

        }
    }
}
