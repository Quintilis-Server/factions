package org.quintilis.factions.handlers

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.services.MemberInviteService
import org.quintilis.factions.services.Services

/**
 * Handler para comandos de membros (/clan member).
 */
class MemberCommandHandler {
    
    private val clanCache get() = Services.clanCache
    private val clanDao get() = Services.clanDao
    private val playerDao get() = Services.playerDao
    private val memberInviteCache get() = Services.memberInviteCache
    
    /**
     * Convida um jogador para o clã.
     * /clan member invite <playerName>
     */
    fun invite(sender: Player, clan: ClanEntity, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val playerEntity = playerDao.findByName(args[0])
        if (playerEntity == null) {
            sender.sendTranslatable("error.player_not_found")
            return
        }
        
        val targetPlayer = playerEntity.getPlayer()
        
        // Verifica se já é membro de algum clã
        if (clanCache.isMember(targetPlayer.uniqueId)) {
            sender.sendTranslatable(
                "error.already_in_a_clan",
                Argument.string("player_name", targetPlayer.name!!)
            )
            return
        }
        
        // Cria o convite
        try {
            val invite = MemberInviteService.createInvite(clan, playerEntity)
            memberInviteCache.put(playerEntity.id, listOf(invite.getClan(clanDao)!!.name))
        } catch (e: Exception) {
            sender.sendTranslatable(
                "error.already_invited",
                Argument.string("name", targetPlayer.name!!)
            )
            return
        }
        
        sender.sendTranslatable(
            "clan.invite.response",
            Argument.string("player_name", targetPlayer.name!!)
        )
    }
    
    /**
     * Expulsa um membro do clã.
     * /clan member kick <playerName>
     */
    fun kick(sender: Player, clan: ClanEntity, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        // TODO: Implementar expulsão de membro
        sender.sendTranslatable("error.not_implemented")
    }
    
    /**
     * Lista membros do clã.
     * /clan member list
     */
    fun list(sender: Player, clan: ClanEntity) {
        val members = clanCache.getMembers(clan.id!!)
        
        sender.sendTranslatable(
            "clan.member.list.header",
            Argument.numeric("count", members.size)
        )
        
        members.forEach { member ->
            val playerName = Bukkit.getOfflinePlayer(member.playerId).name ?: "Unknown"
            val isLeader = member.playerId == clan.leaderUuid
            
            sender.sendTranslatable(
                "clan.member.list.entry",
                Argument.string("player_name", playerName),
                Argument.string("role", if (isLeader) "Leader" else "Member")
            )
        }
    }
    
    /**
     * Retorna sugestões de autocomplete para o subcomando.
     */
    fun getSuggestions(subCommand: String, sender: Player, clan: ClanEntity): List<String> {
        return when (subCommand.lowercase()) {
            "invite" -> {
                val members = clanCache.getMembers(clan.id!!).map { it.playerId }
                Bukkit.getOnlinePlayers()
                    .filter { it.uniqueId !in members && it.uniqueId != sender.uniqueId }
                    .map { it.name }
            }
            "kick" -> {
                clanCache.getMembers(clan.id!!)
                    .filter { it.playerId != clan.leaderUuid }
                    .mapNotNull { Bukkit.getOfflinePlayer(it.playerId).name }
            }
            else -> emptyList()
        }
    }
}
