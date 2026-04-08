package org.quintilis.factions.cache

import org.quintilis.factions.dao.AntiCoreDao
import org.quintilis.factions.entities.clan.AntiCoreEntity

class AntiCoreCache(
    private val daoImpl: AntiCoreDao,
): AbstractDaoCache<AntiCoreDao, AntiCoreEntity, Int>(
    dao = daoImpl,
    ttl = 1200,
    classType = AntiCoreEntity::class.java,
    prefix = "factions:anticore:"
), AntiCoreDao by daoImpl {
}