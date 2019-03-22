package com.mono.api.resources

import com.mono.api.dao.JobDao
import com.mono.api.data.entity.Job
import com.mono.api.data.entity.PartialJob
import com.mono.test.injector
import com.mono.test.job
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RedirectResourceTest {

    @Test
    fun clickJob() {
        val jobDao = injector.getInstance(JobDao::class.java)
        jobDao.dropAndInitTable()
        jobDao.create(PartialJob(job))
        val redirectResource = injector.getInstance(RedirectResource::class.java)

        var result = redirectResource.clickJob(job.id)
        assertEquals(303, result.status)
        assertEquals("${job.applyUrl}", result.getHeaderString("Location"))

        // inactive jobs should no longer redirect to destination
        jobDao.update(job.id, jobDao.updateOf(PartialJob(status = Job.Status.INACTIVE)))
        result = redirectResource.clickJob(job.id)
        assertEquals(303, result.status)
        assertEquals("http://localhost:3000/error/404", result.getHeaderString("Location"))

        // nonexistent jobs should also 404
        result = redirectResource.clickJob(ObjectId())
        assertEquals(303, result.status)
        assertEquals("http://localhost:3000/error/404", result.getHeaderString("Location"))
    }
}