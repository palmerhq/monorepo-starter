package com.mono.api.resources

import io.swagger.annotations.Api
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/v1/status")
@Api("status", description = "Status - basic test if the server is up")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class StatusResource {
    @GET
    fun getStatus(): Response {
        return Response.ok(mapOf("ok" to true)).build()
    }
}