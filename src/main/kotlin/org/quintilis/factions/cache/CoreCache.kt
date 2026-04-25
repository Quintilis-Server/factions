package org.quintilis.factions.cache

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.jdbi.v3.core.HandleConsumer
import org.quintilis.factions.dao.CoreDao
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.services.FactionsServices.clanChunkCache
import redis.clients.jedis.Jedis
import java.lang.Exception

class CoreCache(
    private val coreDao: CoreDao,
    private val plugin: JavaPlugin,
): AbstractDaoCache<CoreDao, ClanCoreEntity, Int>(
    prefix = "factions:core:",
    ttl = 10800,
    classType = ClanCoreEntity::class.java,
    dao = coreDao,
    plugin = plugin
), CoreDao by coreDao {

    private val locationCache = object : BaseRedisCache<String, Int?>(
        keyPrefix = "factions:core:loc:",
        ttlSeconds = 10800,
        plugin = this.plugin
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
        ttlSeconds = 10800,
        plugin = this.plugin
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

        if(cachedId == null) return null

        return findById(cachedId)
    }

    fun hasActiveSubCores(clanId: Int): Boolean {
        val subCores = dao.findAllByClan(clanId)
        println(subCores)
        // Busca todos os núcleos do clã e verifica se existe algum SUB_CORE ativo
        return subCores.any { it.active && it.type == CoreType.SUB_CORE }
    }

    fun isProtectedByOnion(core: ClanCoreEntity): Boolean {
        // 1. REGRA DO NEXUS:
        // Protegido se houver qualquer outro SUB_CORE ativo.
        if (core.type == CoreType.NEXUS) {
            return this.hasActiveSubCores(core.clanId)
        }


        // 2. REGRA DO SUB-CORE:
        // Ele só está protegido se estiver totalmente cercado.
        return isCoreInternal(core)
    }

    private fun isCoreInternal(core: ClanCoreEntity): Boolean {
        val clanId = core.clanId
        val worldUuid = core.worldUuid ?: return false

        // Centro do núcleo em coordenadas de Chunk
        val cx = core.x!! shr 4
        val cz = core.z!! shr 4


        val faceNeighbors = listOf(
            // Face Norte (Z+2)
            Pair(cx - 1, cz + 2), Pair(cx, cz + 2), Pair(cx + 1, cz + 2),
            // Face Sul (Z-2)
            Pair(cx - 1, cz - 2), Pair(cx, cz - 2), Pair(cx + 1, cz - 2),
            // Face Leste (X+2)
            Pair(cx + 2, cz - 1), Pair(cx + 2, cz), Pair(cx + 2, cz + 1),
            // Face Oeste (X-2)
            Pair(cx - 2, cz - 1), Pair(cx - 2, cz), Pair(cx - 2, cz + 1)
        )

        for ((nx, nz) in faceNeighbors) {
            val ownerId = clanChunkCache.getChunkOwner(worldUuid, nx, nz)

            // Se qualquer uma dessas faces for Wilderness ou Inimigo, o núcleo está exposto.
            if (ownerId == null || ownerId != clanId) {
                return false
            }
        }

        return true
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