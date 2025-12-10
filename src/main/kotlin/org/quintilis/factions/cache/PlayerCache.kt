package org.quintilis.factions.cache

import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.entities.player.PlayerEntity
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
    fun getPlayer(uuid: UUID): PlayerEntity? {
        return getOrFetch(uuid) { dbUuid ->
            playerDao.findById(dbUuid)
        }
    }

    fun update(player: PlayerEntity) {
        invalidate(player.id)
    }
}