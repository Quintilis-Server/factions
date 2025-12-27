package org.quintilis.factions.handlers

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.entity.Player
import org.quintilis.factions.entities.log.ActionLogEntity
import org.quintilis.factions.entities.log.ActionLogType
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.services.MemberInviteService
import org.quintilis.factions.services.Services

/**
 * Handler para comandos de convites do jogador (/clan invite).
 * Diferente de MemberCommandHandler, este lida com convites RECEBIDOS pelo jogador.
 */
class InviteCommandHandler {
    
    private val clanCache get() = Services.clanCache
    private val clanDao get() = Services.clanDao
    private val memberInviteDao get() = Services.memberInviteDao
    private val memberInviteCache get() = Services.memberInviteCache
    private val playerCache get() = Services.playerCache
    
    /**
     * Aceita um convite de clã.
     * /clan invite accept <clanName>
     */
    fun accept(sender: Player, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val clan = clanCache.getClanByName(args[0])
        if (clan == null) {
            sender.sendTranslatable("error.no_clan")
            return
        }
        
        // Verifica se já é membro de algum clã
        if (clanCache.isMember(sender.uniqueId)) {
            sender.sendTranslatable(
                "error.already_in_a_clan",
                Argument.string("player_name", sender.name)
            )
            return
        }
        
        // Aceita o convite
        try {
            MemberInviteService.acceptInvite(memberInviteDao, clan, sender)
        } catch (e: Error) {
            sender.sendTranslatable("error.no_member_invite")
            return
        }
        
        // Notifica o líder
        clan.getLeader()?.sendTranslatable(
            "clan.invite.accept.clan_response",
            Argument.string("player_name", sender.name)
        )
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.MEMBER_JOIN,
            actorId = sender.uniqueId,
            clanId = clan.id!!,
            details = "Joined clan: ${clan.name}"
        )
        
        // Invalida cache
        clanCache.invalidateMember(sender.uniqueId)
        clanCache.invalidateMembersOfClan(clan.id)
        
        sender.sendTranslatable(
            "clan.invite.accept.response",
            Argument.string("clan_name", clan.name)
        )
    }
    
    /**
     * Rejeita um convite de clã.
     * /clan invite reject <clanName>
     */
    fun reject(sender: Player, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val clan = clanCache.getClanByName(args[0])
        if (clan == null) {
            sender.sendTranslatable("error.no_clan")
            return
        }
        
        // Rejeita o convite
        try {
            MemberInviteService.rejectInvite(memberInviteDao, clan, sender)
        } catch (e: Error) {
            sender.sendTranslatable("error.no_member_invite")
            return
        }
        
        // Notifica o líder
        clan.getLeader()?.sendTranslatable(
            "clan.invite.reject.clan_response",
            Argument.string("player_name", sender.name)
        )
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.MEMBER_INVITE,
            actorId = sender.uniqueId,
            clanId = clan.id!!,
            details = "Rejected invite from: ${clan.name}"
        )
        
        sender.sendTranslatable(
            "clan.invite.reject.response",
            Argument.string("clan_name", clan.name)
        )
    }
    
    /**
     * Cancela um convite enviado (apenas líder).
     * /clan invite cancel <playerName>
     */
    fun cancel(sender: Player, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val player = playerCache.getPlayer(args[0])
        if (player == null) {
            sender.sendTranslatable("error.player_not_found")
            return
        }
        
        memberInviteDao.cancelInvite(player.id)
        memberInviteCache.invalidate(player.id)
        
        sender.sendTranslatable(
            "clan.invite.cancel.response",
            Argument.string("player_name", player.name)
        )
    }
    
    /**
     * Lista convites pendentes do jogador.
     * /clan invite list
     */
    fun list(sender: Player) {
        val clanNames = memberInviteCache.getClanNames(sender.uniqueId)
        
        if (clanNames.isEmpty()) {
            sender.sendTranslatable("clan.invite.list.empty")
            return
        }
        
        sender.sendTranslatable(
            "clan.invite.list.header",
            Argument.numeric("count", clanNames.size)
        )
        
        clanNames.forEach { clanName ->
            sender.sendTranslatable(
                "clan.invite.list.entry",
                Argument.string("clan_name", clanName)
            )
        }
    }
    
    /**
     * Retorna sugestões de autocomplete para o subcomando.
     */
    fun getSuggestions(subCommand: String, sender: Player): List<String> {
        return when (subCommand.lowercase()) {
            "accept", "reject" -> memberInviteCache.getClanNames(sender.uniqueId)
            "cancel" -> memberInviteCache.getPlayerNames(sender.uniqueId)
            else -> emptyList()
        }
    }
}
