package org.quintilis.factions.cache

import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.entities.player.PlayerEntity
import redis.clients.jedis.Jedis
import java.time.Duration
import java.util.UUID

class PlayerCache(
    private val playerDao: PlayerDao,
): JsonCache<UUID, PlayerEntity>(
    prefix = "factions:player:uuid:",
    ttl = Duration.ofHours(2).seconds,
    classType = PlayerEntity::class.java,
) {
    private val gson = GsonProvider.gson

    private val nameCache = object : BaseRedisCache<String, UUID?>(
        keyPrefix = "factions:player:name:",
        ttlSeconds = Duration.ofHours(2).seconds
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): UUID? {
            val uuidStr = jedis.get(key) ?: return null
            return try {
                UUID.fromString(uuidStr)
            } catch (e: Exception) {
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: UUID?) {
            if (value != null) {
                jedis.set(key, value.toString())
            }
        }

        // Não cachear se o UUID for nulo (jogador não existe)
        override fun shouldCache(value: UUID?): Boolean = value != null
    }

    fun getPlayer(uuid: UUID): PlayerEntity? {
        return getOrFetch(uuid) { dbUuid ->
            playerDao.findById(dbUuid)
        }
    }

    fun getPlayer(name: String): PlayerEntity? {
        // Normalizamos para lowercase para evitar problemas de case-sensitive (Steve vs steve)
        val lowerName = name.lowercase()

        val uuid = nameCache.getOrFetch(lowerName) { _ ->
            // ATENÇÃO: Seu PlayerDao precisa ter um método para buscar por nome
            // Retorna o objeto completo ou apenas o UUID se tiver um método otimizado
            val player = playerDao.findByName(name)
            player?.id
        }

        return if (uuid != null) getPlayer(uuid) else null
    }

    fun update(player: PlayerEntity) {
        invalidate(player.id)
    }
}