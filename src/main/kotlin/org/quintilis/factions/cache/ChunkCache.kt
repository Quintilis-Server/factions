package org.quintilis.factions.cache

import com.google.gson.reflect.TypeToken
import org.bukkit.Chunk
import org.jdbi.v3.core.HandleConsumer
import org.quintilis.factions.dao.ChunkDao
import org.quintilis.factions.entities.chunk.ChunkEntity
import org.quintilis.factions.entities.clan.ClanChunkEntity
import org.quintilis.factions.managers.RedisManager
import redis.clients.jedis.Jedis
import java.util.UUID

/**
 * Cache para chunks de clãs.
 * Cacheia por coordenadas (world:x:z) e por clã.
 */
class ChunkCache(
    private val chunkDao: ChunkDao
): AbstractDaoCache<ChunkDao, ChunkEntity, Int>(
    dao = chunkDao,
    prefix = "factions:chunk:",
    ttl = 1200,
    classType = ChunkEntity::class.java,
), ChunkDao by chunkDao {
    private val gson = GsonProvider.gson
    
    private val chunkListType = object : TypeToken<List<ChunkEntity>>() {}.type
    
    private val CHUNK_TTL = 300L
    private val CLAN_CHUNKS_TTL = 120L
    
    // ============================================
    // Cache de ChunkEntity por coordenadas
    // ============================================
    private val chunkEntityCache = object : BaseRedisCache<String, ChunkEntity?>(
        keyPrefix = "factions:chunk:entity:",
        ttlSeconds = CHUNK_TTL
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): ChunkEntity? {
            val json = jedis.get(key) ?: return null
            if (json == "null") return null
            return try {
                this@ChunkCache.gson.fromJson(json, ChunkEntity::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: ChunkEntity?) {
            val json = if (value != null) this@ChunkCache.gson.toJson(value) else "null"
            jedis.set(key, json)
        }

        override fun shouldCache(value: ChunkEntity?): Boolean = true
    }
    
    // ============================================
    // Cache de chunks por clã
    // ============================================
    private val clanChunksCache = object : BaseRedisCache<Int, List<ChunkEntity>>(
        keyPrefix = "factions:chunk:clan:",
        ttlSeconds = CLAN_CHUNKS_TTL
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): List<ChunkEntity>? {
            val json = jedis.get(key) ?: return null
            if (json.trim() == "[]" || json.isBlank()) return null
            return try {
                this@ChunkCache.gson.fromJson<List<ChunkEntity>>(json, chunkListType)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: List<ChunkEntity>) {
            try {
                val json = this@ChunkCache.gson.toJson(value, chunkListType)
                jedis.set(key, json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun shouldCache(value: List<ChunkEntity>): Boolean = true
    }
    
    // ============================================
    // Cache de contagem de chunks por clã
    // ============================================
    private val countCache = object : BaseRedisCache<Int, Int>(
        keyPrefix = "factions:chunk:count:",
        ttlSeconds = CLAN_CHUNKS_TTL
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): Int? {
            val value = jedis.get(key) ?: return null
            return value.toIntOrNull()
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: Int) {
            jedis.set(key, value.toString())
        }

        override fun shouldCache(value: Int): Boolean = true
    }
    
    // ============================================
    // Métodos públicos de leitura
    // ============================================
    
    /**
     * Cria uma chave de cache a partir das coordenadas.
     */
    private fun coordKey(worldUuid: UUID, x: Int, z: Int): String {
        return "$worldUuid:$x:$z"
    }

    
    /**
     * Busca a entidade de chunk pelas coordenadas.
     */
    fun getChunk(worldUuid: UUID, x: Int, z: Int): ChunkEntity? {
        val key = coordKey(worldUuid, x, z)
        return chunkEntityCache.getOrFetch(key) { _ ->
            chunkDao.findByCoordinates(worldUuid, x, z)
        }
    }
    
    /**
     * Busca todos os chunks de um clã.
     */
    fun getChunksByClan(clanId: Int): List<ChunkEntity> {
        return clanChunksCache.getOrFetch(clanId) { id ->
            chunkDao.findByClanId(id)
        }
    }
    
    /**
     * Retorna a quantidade de chunks de um clã.
     */
    fun getChunkCount(clanId: Int): Int {
        return countCache.getOrFetch(clanId) { id ->
            chunkDao.countByClanId(id)
        }
    }
    
    // ============================================
    // Métodos de invalidação de cache
    // ============================================
    
    /**
     * Invalida o cache de um chunk específico.
     */
    fun invalidateChunk(worldUuid: UUID, x: Int, z: Int) {
        val key = coordKey(worldUuid, x, z)
        chunkEntityCache.invalidate(key)
    }
    
    /**
     * Invalida o cache de chunks de um clã.
     */
    fun invalidateClanChunks(clanId: Int) {
        clanChunksCache.invalidate(clanId)
        countCache.invalidate(clanId)
    }
    
    /**
     * Invalida todos os caches relacionados a um chunk e seu clã.
     */
    fun invalidateChunkAndClan(worldUuid: UUID, x: Int, z: Int, clanId: Int?) {
        invalidateChunk(worldUuid, x, z)
        if (clanId != null) {
            invalidateClanChunks(clanId)
        }
    }
    
    /**
     * Invalida todos os caches de chunks.
     */
    fun invalidateAll() {
        try {
            RedisManager.run { jedis ->
                val keys = jedis.keys("factions:chunk:*")
                if (keys.isNotEmpty()) {
                    jedis.del(*keys.toTypedArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun <X : java.lang.Exception?> useHandle(consumer: HandleConsumer<X?>?) {
        super.useHandle(consumer)
    }
}
