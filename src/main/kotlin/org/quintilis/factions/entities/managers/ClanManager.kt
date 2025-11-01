package org.quintilis.factions.entities.managers

import org.bukkit.entity.Player
import org.quintilis.factions.entities.models.Clan
import org.quintilis.factions.entities.models.ClanMember
import org.quintilis.factions.entities.models.PlayerEntity
import org.quintilis.factions.managers.DatabaseManager
import java.util.*

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

    fun createClan(name: String, tag: String, id: UUID): Clan {
        val clan = Clan(name = name, tag = tag, leaderUuid = id)
        clan.save()
        return clan
    }

    fun getClanByOwner(player: Player): Clan? {
        return DatabaseManager.jdbi.withHandle <Clan, Exception> { handle ->
            handle.createQuery("SELECT * FROM clans WHERE leader_uuid = :uuid")
                .bind("uuid", player.uniqueId)
                .mapTo(Clan::class.java)
                .findOne()
                .orElse(null)
        }
    }

    fun getClanByName(name: String): Clan? {
        return DatabaseManager.jdbi.withHandle <Clan, Exception> {handle ->
            handle.createQuery("SELECT * FROM clans WHERE name = :name")
                .bind("name", name)
                .mapTo(Clan::class.java)
                .findOne()
                .orElse(null)
        }
    }

    fun getMembers(clan: Clan): List<PlayerEntity> {
        return DatabaseManager.jdbi.withHandle<List<PlayerEntity>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT p.* 
                FROM players p
                INNER JOIN clan_members cm ON cm.player_id = p.id
                WHERE cm.clan_id = :clanId
                """
            )
                .bind("clanId", clan.id)
                .mapTo(PlayerEntity::class.java)
                .list()
        }
    }

    fun deleteClan(clan: Clan) {
        DatabaseManager.jdbi.withHandle<Boolean, Exception> { handle ->
            val rowsAffected = handle.createUpdate("DELETE FROM clans WHERE leader_uuid = :uuid")
                .bind("uuid", clan.leaderUuid)
                .execute()

            rowsAffected > 0
        }
    }

    fun removeMember(playerEntity: PlayerEntity, clan: Clan): Boolean {

        return DatabaseManager.jdbi.withHandle<Boolean, Exception> { handle ->

            handle.createUpdate("DELETE FROM clan_members WHERE player_id = :uuid and and clan_id = :clanId")
                .bind("uuid", playerEntity.id)
                .bind("clanId", clan.id)
                .execute()
            true
        }

    }

    fun listClans(): List<Clan> {
        return DatabaseManager.jdbi.withHandle<List<Clan>, Exception> { handle ->
            handle.createQuery("SELECT * FROM clans")
                .mapTo(Clan::class.java)
                .list()
        }
    }

    fun setName(newName: String, clan: Clan) {

        DatabaseManager.jdbi.useHandle<Exception> { handle ->
            handle.createUpdate("UPDATE clans SET name = :name WHERE id = :id")
                .bind("name", newName)
                .bind("id", clan.id)
                .execute()
        }
    }

    fun setTag(newTag: String, clan: Clan) {

        DatabaseManager.jdbi.useHandle<Exception> { handle ->
            handle.createUpdate("UPDATE clans SET tag = :tag WHERE id = :id")
                .bind("tag", newTag)
                .bind("id", clan.id)
                .execute()
        }
    }

    fun existsByName(name: String): Boolean {
        return DatabaseManager.jdbi.withHandle<Boolean, Exception> { handle ->
            handle.createQuery("SELECT 1 FROM clans WHERE name = :name")
                .bind("name", name)
                .mapTo(Boolean::class.java)
                .findOne()
                .orElse(false)
        }
    }

    fun existsByTag(tag: String): Boolean {
        return DatabaseManager.jdbi.withHandle<Boolean, Exception> { handle ->
            handle.createQuery("SELECT 1 FROM clans WHERE tag = :tag")
                .bind("tag", tag)
                .mapTo(Boolean::class.java)
                .findOne()
                .orElse(false)
        }
    }
}