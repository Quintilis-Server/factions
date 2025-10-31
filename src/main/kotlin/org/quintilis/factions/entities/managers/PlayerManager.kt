package org.quintilis.factions.entities.managers


import org.bukkit.entity.Player
import org.quintilis.factions.entities.models.PlayerEntity
import org.quintilis.factions.managers.DatabaseManager

object PlayerManager {

    fun getPlayerUUID(player : Player) : PlayerEntity? {

        return DatabaseManager.jdbi.withHandle<PlayerEntity?, Exception> { handle ->
            handle.createQuery("""
            SELECT p.id, p.name, cm.clan_id AS clanId
            FROM players p
            LEFT JOIN clan_members cm ON cm.player_id = p.id
            WHERE p.id = :id
        """.trimIndent())
                .bind("id", player.uniqueId)
                .mapTo(PlayerEntity::class.java)
                .findOne()
                .orElse(null)
        }
    }
}