package org.quintilis.factions.cache

import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.dao.MemberInviteDao
import org.quintilis.factions.managers.ConfigManager
import java.util.UUID

class MemberInviteCache(
    private val memberInviteDao: MemberInviteDao,
    private val plugin: JavaPlugin,
): StringSetCache<UUID>(
    prefix = "factions:invites:player:",
    ttl = ConfigManager.getMaxInvitationTime() * 60L,
    plugin = plugin
){
    fun getClanNames(playerId: UUID): List<String> {
        // Chama o método da classe pai.
        // Se não tiver no Redis, ele executa o bloco { ... } (o dbFetcher)
        return getOrFetch(playerId) { id ->
            memberInviteDao.findClanNamesForInvites(id)
        }
    }

    fun getPlayerNames(leaderId: UUID): List<String> {
        return getOrFetch(leaderId) {id->
            memberInviteDao.findPlayerNamesForInvites(id)
        }
    }
}