package org.quintilis.factions.cache

import com.google.gson.reflect.TypeToken
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
        JsonCache<String, ChunkEntity>(
                prefix = "factions:chunk:coords:",
                ttl = 600L, // 10 minutos
                classType = ChunkEntity::class.java,
        ) {
    private val gson = GsonProvider.gson
    private val chunkListType = object : TypeToken<List<ChunkEntity>>() {}.type

    // ============================================
    // Cache do clã dono do chunk (chunkId -> clanId)
    // ============================================
    private val ownerCache =
            object :
                    BaseRedisCache<Int, Int?>(
                            keyPrefix = "factions:chunk:owner:",
                            ttlSeconds = 600L
                    ) {
                override fun readFromRedis(jedis: Jedis, key: String): Int? {
                    val value = jedis.get(key) ?: return null
                    return value.toIntOrNull()
                }

                override fun writeToRedis(jedis: Jedis, key: String, value: Int?) {
                    if (value != null) {
                        jedis.set(key, value.toString())
                    }
                }

                override fun shouldCache(value: Int?): Boolean = value != null
            }

    // ============================================
    // Cache de chunks por clã (clanId -> List<ChunkEntity>)
    // ============================================
    private val clanChunksCache =
            object :
                    BaseRedisCache<Int, List<ChunkEntity>>(
                            keyPrefix = "factions:chunk:clan:",
                            ttlSeconds = 300L // 5 minutos
                    ) {
                override fun readFromRedis(jedis: Jedis, key: String): List<ChunkEntity>? {
                    val json = jedis.get(key) ?: return null
                    if (json.trim() == "[]" || json.isBlank()) return null
                    return try {
                        this@ChunkCache.gson.fromJson(json, chunkListType)
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
    private val countCache =
            object :
                    BaseRedisCache<Int, Int>(
                            keyPrefix = "factions:chunk:count:",
                            ttlSeconds = 120L
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

    /** Busca um chunk por suas coordenadas. */
    fun getChunk(worldUuid: UUID, chunkX: Int, chunkZ: Int): ChunkEntity? {
        val coordKey = buildCoordKey(worldUuid, chunkX, chunkZ)
        return getOrFetch(coordKey) { _ -> chunkDao.findByCoordinates(worldUuid, chunkX, chunkZ) }
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
        val coordKey = buildCoordKey(chunk.worldUuid, chunk.chunkX, chunk.chunkZ)
        invalidate(coordKey)

        // Invalida cache do dono se tiver ID
        chunk.id?.let { chunkId -> ownerCache.invalidate(chunkId) }
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
