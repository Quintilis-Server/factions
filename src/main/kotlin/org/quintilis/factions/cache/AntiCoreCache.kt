package org.quintilis.factions.cache

import org.bukkit.Location
import org.quintilis.factions.dao.AntiCoreDao
import org.quintilis.factions.entities.clan.AntiCoreEntity
import redis.clients.jedis.Jedis

class AntiCoreCache(
    private val daoImpl: AntiCoreDao,
): AbstractDaoCache<AntiCoreDao, AntiCoreEntity, Int>(
    dao = daoImpl,
    ttl = 1200,
    classType = AntiCoreEntity::class.java,
    prefix = "factions:anticore:"
), AntiCoreDao by daoImpl {

    private val locationCache = object : BaseRedisCache<String, Int?>(
        keyPrefix = "factions:anticore:loc:",
        ttlSeconds = 10800
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): Int? = jedis.get(key)?.toIntOrNull()
        override fun writeToRedis(jedis: Jedis, key: String, value: Int?) {
            if (value != null) jedis.set(key, value.toString())
        }
        override fun shouldCache(value: Int?): Boolean = value != null
    }

    // 2. Cache na RAM local para evitar spam no Redis (TTL de 5 segundos)
    private val localLocationCache = mutableMapOf<String, Pair<Int?, Long>>()
    private val LOCAL_TTL_MS = 5000L

    fun invalidateSpatialCaches(location: Location) {
        val locKey = genLocKey(location)

        // Limpa Redis
        locationCache.invalidate(locKey)

        // Limpa RAM
        localLocationCache.remove(locKey)
    }

    private fun genLocKey(location: Location): String {
        return "${location.blockX}:${location.blockY}:${location.blockZ}"
    }

    // Sobrescrevendo o findByLocation para usar a hierarquia de cache
    override fun findByLocation(location: Location): AntiCoreEntity? {
        val key = genLocKey(location)
        val now = System.currentTimeMillis()

        // 1. Tenta RAM Local
        val localEntry = localLocationCache[key]
        if (localEntry != null && localEntry.second > now) {
            val cachedId = localEntry.first
            return if (cachedId != null) findById(cachedId) else null
        }

        // 2. Tenta Redis / Banco
        val cachedId = locationCache.getOrFetch(key) { _ ->
            val entity = daoImpl.findByLocation(location)
            entity?.id
        }

        // 3. Salva na RAM local por 5s
        localLocationCache[key] = Pair(cachedId, now + LOCAL_TTL_MS)

        // Limpeza básica do map local se crescer demais
        if (localLocationCache.size > 2000) localLocationCache.clear()

        if (cachedId == null) return null
        return findById(cachedId)
    }
}