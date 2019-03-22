package com.mono.api.util

import java.net.URLDecoder

class StringUtil {
    companion object {
        fun toSlug(name: String?): String? {
            if (name == null) {
                return null
            }
            var slug = name.toLowerCase().trim { it <= ' ' }
            slug = slug.replace(" & ".toRegex(), " and ")
            slug = slug.replace("[ ]+".toRegex(), "-")
            slug = slug.replace("[-]+".toRegex(), "-")
            slug = slug.replace("[^a-z0-9-]".toRegex(), "")
            return slug
        }

        fun decodeUrlencodedForm(string: String): Map<String,Any> {
            val result = mutableMapOf<String,Any>()
            val pairs = string.split("&")
            for (pair in pairs) {
                val keyVal = pair.split("=")
                val key = URLDecoder.decode(keyVal[0], "UTF-8")
                val value = URLDecoder.decode(keyVal[1], "UTF-8")
                result.put(key, value)
            }
            return result.toMap()
        }
    }
}