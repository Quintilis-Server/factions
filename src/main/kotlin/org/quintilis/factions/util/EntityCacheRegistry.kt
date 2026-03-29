package org.quintilis.factions.util

import org.quintilis.factions.entities.BaseEntity
import kotlin.reflect.KClass

interface CacheSync {
    fun updateCache(entity: BaseEntity)
}
object EntityCacheRegistry {
    private val registry = mutableMapOf<KClass<*>, CacheSync>()

    // O Cache chama isso ao iniciar
    fun register(kClass: KClass<*>, cache: CacheSync) {
        registry[kClass] = cache
    }

    // A Entidade chama isso ao salvar
    fun updateCache(entity: BaseEntity) {
        val cache = registry[entity::class]
        cache?.updateCache(entity)
    }
}