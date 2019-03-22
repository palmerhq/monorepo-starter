package com.mono.api.services

import com.google.common.reflect.ClassPath
import com.google.inject.Injector
import io.stardog.stardao.core.Dao
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class DaoService() {
    private val daoMap: MutableMap<String,Dao<*,*,*>> = mutableMapOf()
    private val LOG = LoggerFactory.getLogger(DaoService::class.java)

    fun addDaos(injector: Injector) {
        val daos = ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClasses("com.mono.api.dao")
        for (daoClass in daos) {
            if (!daoClass.toString().contains("Abstract")) {
                val dao = injector.getInstance(daoClass.load()) as Dao<*, *, *>
                daoMap.put(dao.modelClass.toString(), dao)
            }
        }
    }

    fun getAllDaos(): Iterable<Dao<*,*,*>> {
        return daoMap.values
    }
}