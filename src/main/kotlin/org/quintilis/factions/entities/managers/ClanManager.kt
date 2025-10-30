package org.quintilis.factions.entities.managers

import org.bukkit.entity.Player
import org.quintilis.factions.entities.models.Clan
import org.quintilis.factions.entities.models.ClanMember
import org.quintilis.factions.managers.DatabaseManager

object ClanManager {

    fun getClanByPlayer(player: Player): Clan? {
        val member = DatabaseManager.jdbi.withHandle<ClanMember?, Exception> { handle ->
            handle.createQuery("SELECT * FROM clan_members WHERE player_id = :playerId")
                .bind("playerId", player.uniqueId)
                .mapTo(ClanMember::class.java)
                .findOne()
                .orElse(null)
        }

        return member?.let {
            DatabaseManager.jdbi.withHandle<Clan?, Exception> { handle ->
                handle.createQuery("SELECT * FROM clans WHERE id = :id")
                    .bind("id", it.clanId)
                    .mapTo(Clan::class.java)
                    .findOne()
                    .orElse(null)
            }
        }
    }
}