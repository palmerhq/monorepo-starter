package com.mono.api.senders

import io.stardog.dropwizard.worker.WorkMethods
import io.stardog.dropwizard.worker.data.WorkMessage
import io.stardog.dropwizard.worker.interfaces.Sender
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is a sender that will instantly execute any work methods, rather than pass it to a queue, for local usage.
 */
class LocalSender: Sender {
    var workMethods: WorkMethods? = null
    val LOG: Logger = LoggerFactory.getLogger(LocalSender::class.java)

    override fun send(work: WorkMessage) {
        if (workMethods == null) {
            return
        }
        try {
            workMethods!!.getMethod(work.method).consumer.accept(work.params)
        } catch (e: Exception) {
            LOG.error("Error processing ${work.method}(${work.params})", e)
        }
    }
}