package org.quintilis.factions.cache

import com.google.gson.reflect.TypeToken
import org.bukkit.Chunk
import java.util.UUID
import org.quintilis.factions.dao.ChunkDao
import org.quintilis.factions.entities.chunk.ChunkEntity
import org.quintilis.factions.managers.RedisManager
import redis.clients.jedis.Jedis

/**
 * Cache de chunks de territórios.
 *
 * Estratégias de cache:
 * - Por coordenadas: "worldUuid:chunkX:chunkZ" -> ChunkEntity
 * - Por clã dono: chunkId -> clanId
 * - Chunks por clã: clanId -> List<ChunkEntity>
 */
class ChunkCache(private val chunkDao: ChunkDao) :
    AbstractDaoCache<ChunkDao, ChunkEntity, Int>(
        prefix = "factions:chunk:id:",
        ttl = 600L, // 10 minutos
        classType = ChunkEntity::class.java,
        dao = chunkDao
) {
    private val gson = GsonProvider.gson
    private val chunkListType = object : TypeToken<List<ChunkEntity>>() {}.type

    // ============================================
    // Cache do clã dono do chunk (chunkId -> clanId)
    // ============================================
    private val ownerCache = object : BaseRedisCache<Int, Int?>(
        keyPrefix = "factions:chunk:owner:",
        ttlSeconds = 600L
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): Int? {
            return jedis.get(key)?.toIntOrNull()
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: Int?) {
            if (value != null) jedis.set(key, value.toString())
        }

        // Correção: BaseRedisCache geralmente pede o método abstrato, se JsonCache não for usado aqui
        override fun shouldCache(value: Int?): Boolean = value != null
    }

    // ============================================
    // Cache de chunks por clã (clanId -> List<ChunkEntity>)
    // ============================================
    private val clanChunksCache = object : BaseRedisCache<Int, List<ChunkEntity>>(
        keyPrefix = "factions:chunk:clan:",
        ttlSeconds = 300L
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): List<ChunkEntity>? {
            val json = jedis.get(key) ?: return null
            if (json.trim() == "[]" || json.isBlank()) return null
            return try {
                gson.fromJson(json, chunkListType)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: List<ChunkEntity>) {
            val json = gson.toJson(value, chunkListType)
            jedis.set(key, json)
        }

        override fun shouldCache(value: List<ChunkEntity>): Boolean = true
    }

    // ============================================
    // Cache de contagem de chunks por clã
    // ============================================
    private val countCache = object : BaseRedisCache<Int, Int>(
        keyPrefix = "factions:chunk:count:",
        ttlSeconds = 120L
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): Int? = jedis.get(key)?.toIntOrNull()
        override fun writeToRedis(jedis: Jedis, key: String, value: Int) { jedis.set(key, value.toString()) }
        override fun shouldCache(value: Int): Boolean = true
    }

    private val coordCache = object : JsonCache<String, ChunkEntity>(
        prefix = "factions:chunk:coords:",
        ttl = 600L,
        classType = ChunkEntity::class.java
    ) {
        // JsonCache já implementa a serialização, não precisa reescrever
    }

    // ============================================
    // Métodos públicos de leitura
    // ============================================

    fun getChunk(chunk: Chunk): ChunkEntity? {
        return getChunk(chunk.world.uid, chunk.x, chunk.z)
    }

    fun getChunk(worldUuid: UUID, chunkX: Int, chunkZ: Int): ChunkEntity? {
        val coordKey = buildCoordKey(worldUuid, chunkX, chunkZ)

        // CORREÇÃO: Usamos 'coordCache' que aceita String, em vez de 'this' que aceita Int
        return coordCache.getOrFetch(coordKey) { _ ->
            chunkDao.findByCoordinates(worldUuid, chunkX, chunkZ)
        }
    }

    /** Retorna o ID do clã dono de um chunk. */
    fun getClanIdByChunk(chunkId: Int): Int? {
        return ownerCache.getOrFetch(chunkId) { id -> chunkDao.findClanIdByChunkId(id) }
    }

    /** Retorna todos os chunks de um clã. */
    fun getChunksByClan(clanId: Int): List<ChunkEntity> {
        return clanChunksCache.getOrFetch(clanId) { id -> chunkDao.findByClanId(id) }
    }

    /** Retorna a quantidade de chunks de um clã. */
    fun getChunkCount(clanId: Int): Int {
        return countCache.getOrFetch(clanId) { id -> chunkDao.countByClanId(id) }
    }

    /** Verifica se um chunk pertence a algum clã. */
    fun isClaimed(chunkId: Int): Boolean {
        return getClanIdByChunk(chunkId) != null
    }

    // ============================================
    // Métodos de invalidação de cache
    // ============================================

    /** Invalida cache de um chunk específico (por coordenadas e dono). */
    fun invalidateChunk(chunk: ChunkEntity) {
        // 1. Invalida ID (Cache pai)
        chunk.id?.let { invalidate(it) }

        // 2. Invalida Coordenada (Cache interno)
        val coordKey = buildCoordKey(chunk.worldUuid, chunk.chunkX, chunk.chunkZ)
        coordCache.invalidate(coordKey)

        // 3. Invalida Dono
        chunk.id?.let { ownerCache.invalidate(it) }
    }

    /** Invalida todos os caches relacionados a um clã. */
    fun invalidateClanChunks(clanId: Int) {
        clanChunksCache.invalidate(clanId)
        countCache.invalidate(clanId)
    }

    /**
     * Invalida caches após claim/unclaim de chunk. Deve ser chamado após operações de território.
     */
    fun invalidateOnClaimChange(chunk: ChunkEntity, clanId: Int) {
        invalidateChunk(chunk)
        invalidateClanChunks(clanId)
    }

    /** Limpa todos os caches de chunk (use com cuidado). */
    fun invalidateAll() {
        try {
            RedisManager.run { jedis ->
                val patterns =
                        listOf(
                                "factions:chunk:coords:*",
                                "factions:chunk:owner:*",
                                "factions:chunk:clan:*",
                                "factions:chunk:count:*"
                        )
                patterns.forEach { pattern ->
                    val keys = jedis.keys(pattern)
                    if (keys.isNotEmpty()) {
                        jedis.del(*keys.toTypedArray())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ============================================
    // Métodos auxiliares
    // ============================================

    private fun buildCoordKey(worldUuid: UUID, chunkX: Int, chunkZ: Int): String {
        return "$worldUuid:$chunkX:$chunkZ"
    }
}
