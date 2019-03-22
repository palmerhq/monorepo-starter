package com.mono.api.resources

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.annotation.Timed
import com.mono.api.access.JobAccessControl
import com.mono.api.dao.JobDao
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.bson.types.ObjectId
import java.net.URI
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api("redirect", description = "Redirect URLs (clickthroughs for jobs)")
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class RedirectResource @Inject constructor(
        val jobDao: JobDao,
        val accessControl: JobAccessControl,
        val metrics: MetricRegistry,
        @Named("webUrl") val webUrl: String): AbstractResource() {
    val errorCounter404 = metrics.counter(MetricRegistry.name(RedirectResource::class.java, "job", "404"))
    val errorCounter500 = metrics.counter(MetricRegistry.name(RedirectResource::class.java, "job", "500"))
    val successCounter = metrics.counter(MetricRegistry.name(RedirectResource::class.java, "job", "ok"))

    @Timed
    @ApiOperation("Redirect to a job's URL")
    @Path("/job/{jobId}")
    @GET
    fun clickJob(
            @ApiParam("The job id") @PathParam("jobId") jobId: ObjectId
    ): Response {
        try {
            val job = jobDao.loadNullable(jobId);
            if (job == null || !accessControl.canRead(null, job)) {
                errorCounter404.inc()
                return Response.seeOther(URI.create("$webUrl/error/404")).build()
            }
            jobDao.incClickCount(jobId)
            successCounter.inc()
            return Response.seeOther(job.applyUrl).build()
        } catch (e: Exception) {
            errorCounter500.inc()
            return Response.seeOther(URI.create("$webUrl/error/500")).build()
        }
    }
}