package com.mono.api.filters

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter

class Json429Filter: ContainerResponseFilter {
    override fun filter(requestContext: ContainerRequestContext?, responseContext: ContainerResponseContext?) {
        if (responseContext?.status == 429) {
            responseContext.entity = mapOf("error" to "Your request limit was exceeded; please try again later")
            responseContext.headers.add("Content-Type", "application/json")
        }
    }
}