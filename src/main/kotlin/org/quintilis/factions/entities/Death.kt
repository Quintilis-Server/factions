package org.quintilis.factions.entities

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.annotations.Transient
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.services.FactionsServices.playerCache
import java.time.Instant
import java.util.UUID

@TableName("death")
data class Death(
    @PrimaryKey
    val id: Int? = null,
    @Column("player_id")
    val playerId: UUID,
    @Column("killer_id")
    val killerId: UUID,
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
): BaseEntity() {
    fun getPlayer(): OfflinePlayer{
        return Bukkit.getOfflinePlayer(playerId)
    }

    fun getKiller(): OfflinePlayer{
        return Bukkit.getOfflinePlayer(killerId)
    }

    fun getPlayerEntity(): PlayerEntity? {
        return playerCache.getPlayer(playerId)
    }

    fun getKillerEntity(): PlayerEntity? {
        return playerCache.getPlayer(killerId)
    }
}