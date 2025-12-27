package org.quintilis.factions.handlers

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.quintilis.factions.entities.log.ActionLogEntity
import org.quintilis.factions.entities.log.ActionLogType
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.services.Services
import java.util.UUID

/**
 * Handler para comandos de admin de clãs (/clan admin).
 * Permite gerenciar clãs sem ser líder.
 */
class AdminCommandHandler {
    
    private val clanCache get() = Services.clanCache
    private val clanDao get() = Services.clanDao
    private val playerCache get() = Services.playerCache
    
    /**
     * Deleta um clã como admin.
     * /clan admin delete <clanName>
     */
    fun delete(sender: Player, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val clanName = args[0]
        val clan = clanCache.getClanByName(clanName)
        if (clan == null) {
            sender.sendTranslatable(
                "error.clan_not_found",
                Argument.string("clan_name", clanName)
            )
            return
        }
        
        // Busca membros antes de deletar (para invalidar caches)
        val members = clanCache.getMembers(clan.id!!)
        
        // Deleta o clã
        try {
            clanDao.softDeleteClan(clan.id)
            clanDao.deactivateClanMembers(clan.id)
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendTranslatable("error.generic")
            return
        }
        
        // Invalida caches
        clanCache.invalidateClan(clan)
        members.forEach { clanCache.invalidateMember(it.playerId) }
        clanCache.invalidateGlobalCaches()
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.ADMIN_CLAN_DELETE,
            actorId = sender.uniqueId,
            clanId = clan.id,
            details = "Admin deleted clan: ${clan.name}"
        )
        
        // Notifica líder se online
        clan.getLeader()?.sendTranslatable(
            "clan.admin.delete.target_response",
            Argument.string("admin_name", sender.name)
        )
        
        sender.sendTranslatable(
            "clan.admin.delete.response",
            Argument.string("clan_name", clan.name)
        )
    }
    
    /**
     * Altera o nome de um clã.
     * /clan admin setname <clanName> <newName>
     */
    fun setName(sender: Player, args: List<String>) {
        if (args.size < 2) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val clanName = args[0]
        val newName = args[1]
        
        val clan = clanCache.getClanByName(clanName)
        if (clan == null) {
            sender.sendTranslatable(
                "error.clan_not_found",
                Argument.string("clan_name", clanName)
            )
            return
        }
        
        // Verifica se o novo nome já existe
        if (clanCache.existsByName(newName)) {
            sender.sendTranslatable(
                "clan.create.error.already_exists",
                Argument.string("clan_name", newName)
            )
            return
        }
        
        val oldName = clan.name
        
        // Atualiza o nome
        try {
            clanDao.updateName(clan.id!!, newName)
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendTranslatable("error.generic")
            return
        }
        
        // Invalida cache
        clanCache.invalidateClan(clan)
        clanCache.invalidateGlobalCaches()
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.ADMIN_CLAN_SET_NAME,
            actorId = sender.uniqueId,
            clanId = clan.id,
            details = "Admin changed clan name from '$oldName' to '$newName'"
        )
        
        sender.sendTranslatable(
            "clan.admin.setname.response",
            Argument.string("old_name", oldName),
            Argument.string("new_name", newName)
        )
    }
    
    /**
     * Altera a tag de um clã.
     * /clan admin settag <clanName> <newTag>
     */
    fun setTag(sender: Player, args: List<String>) {
        if (args.size < 2) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val clanName = args[0]
        val newTag = args[1]
        
        val clan = clanCache.getClanByName(clanName)
        if (clan == null) {
            sender.sendTranslatable(
                "error.clan_not_found",
                Argument.string("clan_name", clanName)
            )
            return
        }
        
        // Verifica se a tag já existe
        val existingTags = clanDao.listTags(clan.id!!)
        if (existingTags.contains(newTag)) {
            sender.sendTranslatable(
                "clan.admin.settag.error.exists",
                Argument.string("tag", newTag)
            )
            return
        }
        
        val oldTag = clan.tag ?: ""
        
        // Atualiza a tag
        try {
            clanDao.updateTag(clan.id, newTag)
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendTranslatable("error.generic")
            return
        }
        
        // Invalida cache
        clanCache.invalidateClan(clan)
        clanCache.invalidateGlobalCaches()
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.ADMIN_CLAN_SET_TAG,
            actorId = sender.uniqueId,
            clanId = clan.id,
            details = "Admin changed clan tag from '$oldTag' to '$newTag'"
        )
        
        sender.sendTranslatable(
            "clan.admin.settag.response",
            Argument.string("clan_name", clan.name),
            Argument.string("new_tag", newTag)
        )
    }
    
    /**
     * Altera o líder de um clã.
     * /clan admin setleader <clanName> <playerName>
     */
    fun setLeader(sender: Player, args: List<String>) {
        if (args.size < 2) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val clanName = args[0]
        val playerName = args[1]
        
        val clan = clanCache.getClanByName(clanName)
        if (clan == null) {
            sender.sendTranslatable(
                "error.clan_not_found",
                Argument.string("clan_name", clanName)
            )
            return
        }
        
        val newLeader = playerCache.getPlayer(playerName)
        if (newLeader == null) {
            sender.sendTranslatable("error.player_not_found")
            return
        }
        
        val oldLeaderId = clan.leaderUuid
        
        // Atualiza o líder
        try {
            clanDao.updateLeader(clan.id!!, newLeader.id)
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendTranslatable("error.generic")
            return
        }
        
        // Invalida cache
        clanCache.invalidateClan(clan)
        clanCache.invalidateGlobalCaches()
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.ADMIN_CLAN_SET_LEADER,
            actorId = sender.uniqueId,
            clanId = clan.id,
            details = "Admin changed clan leader to '${newLeader.name}'"
        )
        
        // Notifica novo líder se online
        Bukkit.getPlayer(newLeader.id)?.sendTranslatable(
            "clan.admin.setleader.target_response",
            Argument.string("clan_name", clan.name),
            Argument.string("admin_name", sender.name)
        )
        
        // Notifica antigo líder se online
        Bukkit.getPlayer(oldLeaderId)?.sendTranslatable(
            "clan.admin.setleader.old_leader_response",
            Argument.string("clan_name", clan.name),
            Argument.string("new_leader", newLeader.name)
        )
        
        sender.sendTranslatable(
            "clan.admin.setleader.response",
            Argument.string("clan_name", clan.name),
            Argument.string("new_leader", newLeader.name)
        )
    }
    
    /**
     * Retorna sugestões de autocomplete para o subcomando.
     */
    fun getSuggestions(subCommand: String): List<String> {
        return when (subCommand.lowercase()) {
            "delete", "setname", "settag", "setleader" -> clanDao.findNames()
            else -> emptyList()
        }
    }
}
