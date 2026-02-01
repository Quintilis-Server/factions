package org.quintilis.factions.cache

import org.quintilis.factions.dao.BaseDao
import org.quintilis.factions.entities.BaseEntity

abstract class AbstractDaoCache<D: BaseDao<E, K>, E: BaseEntity, K>(
    protected val dao: D,
    prefix: String,
    ttl: Long,
    classType: Class<E>
): JsonCache<K, E>(prefix, ttl, classType) {
    /**
     * Implementação padrão do findById.
     * Tenta Redis -> Se falhar, vai no DAO -> Salva no Redis -> Retorna.
     */
    open fun findById(id: K): E? {
        return getOrFetch(id) { dbId ->
            dao.findById(dbId)
        }
    }

    // Você pode adicionar métodos utilitários de update aqui se quiser
    open fun invalidateAndReload(id: K): E? {
        invalidate(id)
        return findById(id)
    }
}