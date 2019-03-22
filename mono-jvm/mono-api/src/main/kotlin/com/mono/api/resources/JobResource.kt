package com.mono.api.resources

import com.mono.api.access.JobAccessControl
import com.mono.api.auth.UserAuth
import com.mono.api.dao.CustomerDao
import com.mono.api.dao.JobDao
import com.mono.api.data.entity.PartialJob
import com.mono.api.data.entity.Job
import io.dropwizard.auth.Auth
import io.stardog.stardao.core.Results
import io.stardog.stardao.core.Update
import io.stardog.stardao.validation.DefaultValidator
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.jaxrs.PATCH
import org.bson.types.ObjectId
import java.util.*
import java.net.URI
import java.time.Instant
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api("job", description = "Operations relating to jobs")
@Path("/v1/job")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class JobResource @Inject constructor(
        val jobDao: JobDao,
        val customerDao: CustomerDao,
        val accessControl: JobAccessControl): AbstractResource() {

    @ApiOperation("Return a job by id")
    @Path("/{jobId}")
    @GET
    fun getJob(
            @ApiParam(hidden = true) @Auth auth: UserAuth?,
            @ApiParam("The job id") @PathParam("jobId") jobId: ObjectId
    ): PartialJob {
        val job = jobDao.load(jobId);
        if (!accessControl.canRead(auth, job)) {
            throw NotFoundException("Job not found: $jobId");
        }
        return accessControl.toVisible(auth, job);
    }

    @ApiOperation("Find jobs, either all active or by customer id")
    @GET
    fun findJobs(
            @ApiParam(hidden = true) @Auth authOpt: Optional<UserAuth>,
            @ApiParam("Customer id to filter by") @QueryParam("customerId") customerId: ObjectId?,
            @ApiParam("Job to iterate from") @QueryParam("from") from: ObjectId?,
            @ApiParam("Number of results to return") @QueryParam("limit") limit: Int?
    ): Results<PartialJob, ObjectId?> {
        val auth = authOpt.orElse(null)
        val limitNum = parseLimit(limit, 20, 100)
        val iterable: Iterable<Job>      
        if (customerId != null) {
            if (!accessControl.hasCustomerAccess(auth, customerId)) {
                throw ForbiddenException("Permission denied")
            }
            iterable = jobDao.iterateByCustomer(customerId, from)            
        } else {
            iterable = jobDao.iterateActive(from)
        }
        return accessControl.toVisibleResults(auth, iterable, limitNum, { it.id!! })
    }

    @ApiOperation("Create a job", code = 201, response = Job::class)
    @POST
    fun createJob(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            create: PartialJob
    ): Response {
        val createVal = create.copy(
                status = create.status ?: Job.Status.ACTIVE
        )
        DefaultValidator.validateCreate(createVal, jobDao)
        if (!accessControl.canCreate(auth, createVal)) {
            throw ForbiddenException("Permission denied")
        }

        val customer = customerDao.load(create.customerId!!)
        val jobCount = jobDao.countActiveByCustomer(create.customerId)
        if (jobCount >= customer.maxJobs) {
            throw ForbiddenException("You have only purchased ${customer.maxJobs} jobs")
        }

        val created = jobDao.create(createVal, Instant.now(), auth.user.id)
        return Response.created(URI.create("/v1/job/" + created.id)).entity(created).build()
    }

    @ApiOperation("Update a job by id")
    @Path("/{jobId}")
    @PATCH
    fun updateJob(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @ApiParam("Job id") @PathParam("jobId") jobId: ObjectId,
            update: Update<PartialJob>
    ): Response {
        DefaultValidator.validateUpdate(update, jobDao)
        val existing = jobDao.load(jobId)
        if (!accessControl.canUpdate(auth, existing, update)) {
            throw ForbiddenException("Permission denied")
        }
        if (existing.status != Job.Status.ACTIVE && update.partial?.status == Job.Status.ACTIVE) {
            val customer = customerDao.load(existing.customerId)
            val jobCount = jobDao.countActiveByCustomer(existing.customerId)
            if (jobCount >= customer.maxJobs) {
                throw ForbiddenException("You have only purchased ${customer.maxJobs} jobs")
            }
        }
        jobDao.update(jobId, update, Instant.now(), ObjectId());
        return Response.noContent().build();
    }

    @ApiOperation("Delete a job by id")
    @Path("/{jobId}")
    @DELETE
    fun deleteJob(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @ApiParam("Job id") @PathParam("jobId") jobId: ObjectId
    ): Response {
        val existing = jobDao.load(jobId)
        if (!accessControl.canDelete(auth, existing)) {
            throw ForbiddenException("Permission denied")
        }
        jobDao.softDelete(jobId, auth.user.id)
        return Response.noContent().build();
    }
}