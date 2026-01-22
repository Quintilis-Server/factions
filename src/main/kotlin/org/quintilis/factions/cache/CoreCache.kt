package org.quintilis.factions.cache

import org.quintilis.factions.dao.CoreDao
import org.quintilis.factions.entities.clan.ClanCoreEntity

class CoreCache(private val coreDao: CoreDao): JsonCache<String, ClanCoreEntity>(
    prefix = "core",
    ttl = 1200,
    classType = ClanCoreEntity::class.java,
) {
//    fun invalidateCore(core: ClanCoreEntity){
//        coreDao.delete(core);
//    }
}