package com.mono.api.resources

import com.codahale.metrics.annotation.Timed
import io.dropwizard.auth.Auth
import com.mono.api.auth.UserAuth
import com.mono.api.dao.MediaDao
import com.mono.api.data.entity.Media
import io.stardog.starwizard.services.media.MediaService
import io.stardog.starwizard.services.media.MediaUtil
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.apache.commons.lang3.RandomStringUtils
import org.bson.types.ObjectId
import org.glassfish.jersey.media.multipart.FormDataBodyPart
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Singleton
@Path("/v1/media")
class MediaResource @Inject constructor(
        private val mediaDao: MediaDao,
        private val mediaService: MediaService
) {
    private val ALLOWED_TYPES = setOf("image/jpeg", "image/png", "image/gif")

    @POST
    @Timed
    @ApiOperation(value = "Upload a new piece of media", response = Media::class, code = 201)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadMedia(
            @ApiParam(hidden = true) @Auth auth: UserAuth,
            @FormDataParam("file") fileInputStream: InputStream,
            @FormDataParam("file") bodyPart: FormDataBodyPart
    ): Response {
        val mediaTypeString = bodyPart.mediaType.toString()
        if (!ALLOWED_TYPES.contains(mediaTypeString)) {
            throw BadRequestException("Type not permitted: $mediaTypeString")
        }

        // copy stream to local file
        val ext = MediaUtil.toExtension(bodyPart.mediaType)
        val localFile = File("/tmp/" + RandomStringUtils.randomAlphanumeric(32) + "." + ext)
        Files.copy(fileInputStream, localFile.toPath())

        try {
            val mediaId = ObjectId()
            val now = Instant.now()
            val todayUtc = now.atZone(ZoneId.of("UTC")).toLocalDateTime()
            val storagePath = "image/" + auth.user.id + "/" + todayUtc.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/$mediaId.$ext"
            val image = mediaService.uploadImage(localFile, bodyPart.mediaType, storagePath, "50x50sq")

            val media = mediaDao.create(Media(
                    id = mediaId,
                    path = storagePath,
                    type = bodyPart.mediaType.toString(),
                    bytes = image.bytes,
                    width = image.width,
                    height = image.height,
                    versions = image.versions.map { it.name }.toSet(),
                    createAt = now,
                    createId = auth.user.id
            ))

            return Response.created(URI.create("/v1/media/" + mediaId)).entity(media).build()
        } catch (e: Exception) {
            localFile.delete()
            throw e
        }
    }

}