package org.quintilis.factions.entities.managers

import org.quintilis.factions.entities.models.PlayerEntity
import org.quintilis.factions.managers.DatabaseManager
import java.util.*

object PlayerManager {

    fun getPlayerUUID(Uuid : UUID) : PlayerEntity {

        return DatabaseManager.jdbi.withHandle<PlayerEntity, Exception> { handle ->
            handle.createQuery("""
            SELECT p.id, p.name, cm.clan_id AS clanId
            FROM players p
            LEFT JOIN clan_members cm ON cm.player_id = p.id
            WHERE p.id = :id
        """.trimIndent())
                .bind("id", Uuid)
                .mapTo(PlayerEntity::class.java)
                .findOne()
                .orElse(null)
        }
    }

    fun getPlayerByName(name: String): PlayerEntity? {
        return DatabaseManager.jdbi.withHandle<PlayerEntity?, Exception> { handle ->
            handle.createQuery("SELECT * FROM players WHERE name = :name")
                .bind("name", name)
                .mapTo(PlayerEntity::class.java)
                .findOne()
                .orElse(null)
        }
    }
}