package com.mono.api.exceptionmappers

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

class IllegalArgumentExceptionMapper : ExceptionMapper<IllegalArgumentException> {
    override fun toResponse(e: IllegalArgumentException): Response {
        var message = if (e.message != null) e.message else "Invalid request format"
        if (message?.startsWith("Parameter specified as non-null") == true) {
            message = "Missing required parameter: " + message.substring(message.indexOf(", parameter") + 12)
        }
        return Response.status(Response.Status.BAD_REQUEST).type("application/json")
                .entity(mapOf("message" to message))
                .build()
    }
}
