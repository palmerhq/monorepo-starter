package com.mono.api.senders

import io.stardog.dropwizard.worker.data.WorkMessage
import io.stardog.dropwizard.worker.interfaces.Sender
import javax.inject.Singleton

/**
 * This is a sender that will keep track of the last workMethod it was sent, for testing purposes.
 */
@Singleton
class TestSender: Sender {
    var lastWorkMessage: WorkMessage? = null

    override fun send(work: WorkMessage) {
        lastWorkMessage = work
    }

    fun reset() {
        lastWorkMessage = null
    }
}