package org.quintilis.factions.cache

import org.bukkit.Location
import org.jdbi.v3.core.HandleConsumer
import org.quintilis.factions.dao.CoreDao
import org.quintilis.factions.entities.clan.ClanCoreEntity
import redis.clients.jedis.Jedis
import java.lang.Exception

class CoreCache(private val coreDao: CoreDao): AbstractDaoCache<CoreDao, ClanCoreEntity, Int>(
    prefix = "core",
    ttl = 1200,
    classType = ClanCoreEntity::class.java,
    dao = coreDao
), CoreDao by coreDao {

    private val locationCache = object : BaseRedisCache<String, Int?>(
        keyPrefix = "factions:core:loc:",
        ttlSeconds = 10800
    ){
        override fun readFromRedis(jedis: Jedis, key: String): Int? {
            return jedis.get(key).toIntOrNull()
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: Int?) {
            if (value != null) {
                jedis.set(key, value.toString())
            }
        }

        override fun shouldCache(value: Int?): Boolean = value != null
    }

    override fun <X : Exception?> useHandle(consumer: HandleConsumer<X?>?) {
        super<CoreDao>.useHandle(consumer)
    }

    override fun findById(id: Int): ClanCoreEntity? {
        return super<AbstractDaoCache>.findById(id)
    }

    private val localLocationCache = mutableMapOf<String, Pair<Int?, Long>>()
    private val LOCAL_TTL_MS = 5000L

    fun findByLocation(location: Location): ClanCoreEntity? {
        val key = genLocKey(location)
        val now = System.currentTimeMillis()

        val localEntry = localLocationCache[key]

        if(localEntry != null && localEntry.second > now) {
            val cachedId = localEntry.first
            return if (cachedId != null) findById(cachedId) else null
        }

        val cachedId = locationCache.getOrFetch(key) { _ ->
            val core = coreDao.findByLocation(
                location.blockX,
                location.blockY,
                location.blockZ
            )
            core?.id
        }

        localLocationCache[key] = Pair(cachedId, now + LOCAL_TTL_MS)

        if(localLocationCache.size > 1000) localLocationCache.clear()

        if(cachedId == null) return null

        return findById(cachedId)
    }

    override fun invalidate(key: Int) {
        super.invalidate(key)

    }

    private fun genLocKey(location: Location): String {
        return "${location.blockX}:${location.blockY}:${location.blockZ}"
    }

}