package org.quintilis.factions.cache

import com.google.common.reflect.TypeToken
import org.bukkit.Chunk
import org.quintilis.factions.dao.ClanChunkDao
import org.quintilis.factions.entities.chunk.ChunkEntity
import org.quintilis.factions.entities.clan.ClanChunkEntity
import org.quintilis.factions.managers.GsonProvider
import redis.clients.jedis.Jedis
import java.util.UUID

class ClanChunkCache(
    private val daoImpl: ClanChunkDao,
): AbstractDaoCache<ClanChunkDao, ClanChunkEntity, Int>(
    dao = daoImpl,
    prefix = "factions:clan_chunk:",
    ttl = 1200L,
    classType = ClanChunkEntity::class.java,
), ClanChunkDao by daoImpl {
    private val gson = GsonProvider.gson
    private val chunkListType = object : TypeToken<List<ChunkEntity>>() {}.type

    private val coreChunksCache = object : BaseRedisCache<Int, List<ChunkEntity>>(
        keyPrefix = "factions:clan_chunk:core:",
        ttlSeconds = 300L
    ) {
        override fun readFromRedis(jedis: redis.clients.jedis.Jedis, key: String): List<ChunkEntity>? {
            val json = jedis.get(key) ?: return null
            if (json.trim() == "[]" || json.isBlank()) return null
            return try {
                this@ClanChunkCache.gson.fromJson(json, chunkListType)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: redis.clients.jedis.Jedis, key: String, value: List<ChunkEntity>) {
            try {
                val json = this@ClanChunkCache.gson.toJson(value, chunkListType)
                jedis.set(key, json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun shouldCache(value: List<ChunkEntity>): Boolean = true
    }

    private val ownerCache = object : BaseRedisCache<String, Int?>(
        keyPrefix = "factions:clan_chunk:owner:",
        ttlSeconds = 300L
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

//    private val coreChunksCache = object : BaseRedisCache<Int, List<ChunkEntity>>(
//        keyPrefix = "factions:clan_chunk:core:",
//        ttlSeconds = 300L
//    ) {
//        override fun readFromRedis(jedis: Jedis, key: String): List<ChunkEntity>? {
//            val json = jedis.get(key) ?: return null
//            if (json.trim() == "[]" || json.isBlank()) return null
//            return try {
//                this@ClanChunkCache.gson.fromJson(json, chunkListType)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                null
//            }
//        }
//
//        override fun writeToRedis(jedis: Jedis, key: String, value: List<ChunkEntity>) {
//            try {
//                val json = this@ClanChunkCache.gson.toJson(value, chunkListType)
//                jedis.set(key, json)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//
//        override fun shouldCache(value: List<ChunkEntity>): Boolean = true
//    }

    private fun coordKey(worldUuid: UUID, x: Int, z: Int): String {
        return "$worldUuid:$x:$z"
    }
    /**
     * Busca o ID do clã dono de um chunk pelas coordenadas.
     * Retorna null se o chunk não pertence a nenhum clã.
     */
    fun getChunkOwner(worldUuid: UUID, x: Int, z: Int): Int? {
        val key = coordKey(worldUuid, x, z)
        return ownerCache.getOrFetch(key) { _ ->
            // Chama o daoImpl para buscar no PostgreSQL se não tiver no Redis
            daoImpl.findClanIdByCoordinates(worldUuid, x, z)
        }
    }

    fun getChunkOwner(chunkEntity: ChunkEntity): Int? {
        val chunk = chunkEntity.getChunk()
        return this.getChunkOwner(chunk!!.world.uid, chunk.x, chunk.z)
    }

    /**
     * Verifica se um chunk está reivindicado.
     */
    fun isClaimed(worldUuid: UUID, x: Int, z: Int): Boolean {
        return getChunkOwner(worldUuid, x, z) != null
    }

    fun isClaimed(chunk: Chunk): Boolean {
        return this.isClaimed(chunk.world.uid,chunk.x, chunk.z)
    }

    /**
     * Verifica se um chunk pertence a um clã específico.
     */
    fun isOwnedBy(worldUuid: UUID, x: Int, z: Int, clanId: Int): Boolean {
        return getChunkOwner(worldUuid, x, z) == clanId
    }
}