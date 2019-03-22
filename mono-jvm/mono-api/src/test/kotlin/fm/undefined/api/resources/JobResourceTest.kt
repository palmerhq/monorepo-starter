package com.mono.api.resources

import com.mono.api.dao.CustomerDao
import com.mono.api.dao.JobDao
import com.mono.api.data.entity.Job
import com.mono.api.data.entity.PartialCustomer
import com.mono.api.data.entity.PartialJob
import com.mono.test.*
import io.stardog.stardao.jackson.JsonHelper
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach
import java.util.*
import javax.ws.rs.NotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class JobResourceTest {
    val jobDao = injector.getInstance(JobDao::class.java)
    val customerDao = injector.getInstance(CustomerDao::class.java)
    val resource = injector.getInstance(JobResource::class.java)

    @BeforeEach
    internal fun setUp() {
        jobDao.dropAndInitTable()
        jobDao.create(PartialJob(job))
        customerDao.dropAndInitTable()
        customerDao.create(PartialCustomer(customer))
    }

    @Test
    fun getJob() {
        var job = resource.getJob(null, job.id)
        assertEquals("Test Job", job.title)
        assertNull(job.customerId)

        // ensure anon users cannot read INACTIVE jobs
        jobDao.update(job.id, jobDao.updateOf(PartialJob(status = Job.Status.INACTIVE)))
        assertFailsWith<NotFoundException> {
            resource.getJob(null, job.id!!)
        }
        job = resource.getJob(userAuth, job.id!!)
        assertEquals(customerId, job.customerId)
    }

    @Test
    fun findJobs() {
        val jobs = resource.findJobs(Optional.empty(), null, null, null)
        assertEquals(1, jobs.data.size)
        assertEquals("Test Job", jobs.data[0].title)
    }

    @Test
    fun createJob() {
        val created = resource.createJob(userAuth, JsonHelper.obj(
                "{title:'New Job',customerId:'${customerId}',location:'New York, NY',remoteFriendly:true,companyName:'NewCo',description:'This is a cool job',type:'FULL',category:'DEVELOPMENT',companyUrl:'http://example.com',applyUrl:'https://careers.example.com/jobs/1234'}", PartialJob::class.java))
        assertEquals(201, created.status)
        val createdJob = created.entity as Job
        assertEquals("New Job", createdJob.title)
        assertEquals(0, createdJob.clickCount)
    }

    @Test
    fun updateJob() {
        val response = resource.updateJob(userAuth, job.id, JsonHelper.update(
                "{title:'Job Rename'}", PartialJob::class.java))
        assertEquals(204, response.status)

        val load = resource.getJob(null, job.id)
        assertEquals("Job Rename", load.title)

    }

    @Test
    fun deleteJob() {
        val response = resource.deleteJob(userAuth, job.id)
        assertEquals(204, response.status)

        assertFailsWith<NotFoundException> {
            resource.getJob(null, job.id)
        }
        assertEquals(0, resource.findJobs(Optional.empty(), null, null, null).data.size)
    }
}