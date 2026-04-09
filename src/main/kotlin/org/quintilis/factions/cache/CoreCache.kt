package org.quintilis.factions.cache

import org.bukkit.Chunk
import org.bukkit.Location
import org.jdbi.v3.core.HandleConsumer
import org.quintilis.factions.dao.CoreDao
import org.quintilis.factions.entities.clan.ClanCoreEntity
import redis.clients.jedis.Jedis
import java.lang.Exception

class CoreCache(private val coreDao: CoreDao): AbstractDaoCache<CoreDao, ClanCoreEntity, Int>(
    prefix = "factions:core:",
    ttl = 10800,
    classType = ClanCoreEntity::class.java,
    dao = coreDao
), CoreDao by coreDao {

    private val locationCache = object : BaseRedisCache<String, Int?>(
        keyPrefix = "factions:core:loc:",
        ttlSeconds = 10800
    ){
        override fun readFromRedis(jedis: Jedis, key: String): Int? {
            return jedis.get(key)?.toIntOrNull()
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


    private val chunkPosCache = object : BaseRedisCache<String, Int?>(
        keyPrefix = "factions:core:chunk_pos:",
        ttlSeconds = 10800
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): Int? {
            val value = jedis.get(key) ?: return null
            if (value == "null") return null
            return value.toIntOrNull()
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: Int?) {
            jedis.set(key, value?.toString() ?: "null")
        }

        override fun shouldCache(value: Int?): Boolean = true
    }

    private val localChunkCache = mutableMapOf<String, Pair<Int?, Long>>()

    private val localLocationCache = mutableMapOf<String, Pair<Int?, Long>>()
    private val LOCAL_TTL_MS = 5000L

    override fun invalidate(key: Int) {
        super.invalidate(key)

    }

    fun invalidateSpatialCaches(location: Location, chunk: Chunk) {
        val locKey = genLocKey(location)
        val chunkKey = "${chunk.world.uid}:${chunk.x}:${chunk.z}"

        locationCache.invalidate(locKey)
        chunkPosCache.invalidate(chunkKey)

        localLocationCache.remove(locKey)
        localChunkCache.remove(chunkKey)
    }

    private fun genLocKey(location: Location): String {
        return "${location.blockX}:${location.blockY}:${location.blockZ}"
    }


    override fun findByLocation(location: Location): ClanCoreEntity? {
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

//    override fun findByChunk(chunk: Chunk): ClanCoreEntity? {
//        val key = "${chunk.world.uid}:${chunk.x}:${chunk.z}"
//        val now = System.currentTimeMillis()
//
//        // 1. Tenta achar na memória RAM local (super rápido)
//        val localEntry = localChunkCache[key]
//        if(localEntry != null && localEntry.second > now) {
//            val cachedId = localEntry.first
//            return if (cachedId != null) findById(cachedId) else null
//        }
//
//        // 2. Tenta achar no Redis (ou faz a Query com JOIN no Banco)
//        val cachedId = chunkPosCache.getOrFetch(key) { _ ->
//            // ATENÇÃO: Certifique-se de adicionar essa query no seu CoreDao!
//            val core = coreDao.findByChunkCoords(chunk.world.uid, chunk.x, chunk.z)
//            core?.id
//        }
//
//        // 3. Salva no cache local para as próximas batidas
//        localChunkCache[key] = Pair(cachedId, now + LOCAL_TTL_MS)
//        if(localChunkCache.size > 1000) localChunkCache.clear()
//
//        if(cachedId == null) return null
//        return findById(cachedId)
//    }
}