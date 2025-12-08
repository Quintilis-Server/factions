package org.quintilis.factions.cache

import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.entities.clan.ClanEntity

class ClanCache(
    private val clanDao: ClanDao
): JsonCache<Int, ClanEntity>(
    prefix = "factions:clan:id:",
    ttl = 300L,
    classType = ClanEntity::class.java,
) {
    /**
     * Busca um clã pelo ID.
     * 1. Tenta Redis.
     * 2. Se falhar, busca no Postgres via ClanDao.
     * 3. Salva no Redis e retorna.
     */
    fun getClan(id: Int): ClanEntity? {
        return getOrFetch(id) { dbId ->
            clanDao.findById(dbId)
        }
    }

    fun update(clan: ClanEntity) {
        if (clan.id == null) return

        invalidate(clan.id)
    }
}