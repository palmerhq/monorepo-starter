package com.mono.api.resources

import javax.ws.rs.BadRequestException

abstract class AbstractResource {
    fun parseLimit(limit: Int?, default: Int, max: Int): Int {
        if (limit == null) {
            return default
        }
        if (limit < 1) {
            throw BadRequestException("limit must be at least 1")
        }
        if (limit > max) {
            throw BadRequestException("limit may not be more than $max")
        }
        return limit
    }
}