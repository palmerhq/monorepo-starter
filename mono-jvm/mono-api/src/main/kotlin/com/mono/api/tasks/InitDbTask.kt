package com.mono.api.tasks

import com.mono.api.services.DaoService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitDbTask @Inject constructor(val daoService: DaoService): Consumer<Map<String,Any?>> {
    private val LOG: Logger = LoggerFactory.getLogger(InitDbTask::class.java)

    override fun accept(params: Map<String, Any?>) {
        for (dao in daoService.getAllDaos()) {
            LOG.info("Initializing table: ${dao.javaClass}")
            dao.initTable()
        }
    }
}