package org.quintilis.factions.handlers

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanRelationEntity
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.commands.clan.AllySubCommands
import org.quintilis.factions.services.AllyInviteService
import org.quintilis.factions.services.Services

/**
 * Handler para comandos de aliança (/clan ally).
 */
class AllyCommandHandler {
    
    private val clanCache get() = Services.clanCache
    private val clanDao get() = Services.clanDao
    private val clanRelationDao get() = Services.clanRelationDao
    private val allyInviteDao get() = Services.allyInviteDao
    private val allyInviteCache get() = Services.allyInviteCache
    
    /**
     * Adiciona uma aliança (envia convite).
     * /clan ally add <clanName>
     */
    fun add(sender: Player, clan: ClanEntity, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val targetClan = clanCache.getClanByName(args[0])
        if (targetClan == null) {
            sender.sendTranslatable("error.no_clan")
            return
        }
        
        // Não pode se aliar ao próprio clã
        if (targetClan.id == clan.id) {
            sender.sendTranslatable("clan.ally.error.same_clan")
            return
        }
        
        // Verifica se já são aliados
        if (clanRelationDao.isRelation(clan.id!!, targetClan.id!!, Relation.ALLY)) {
            sender.sendTranslatable("clan.ally.error.is_ally")
            return
        }
        
        // Verifica se são inimigos
        if (clanRelationDao.isRelation(clan.id, targetClan.id, Relation.ENEMY)) {
            sender.sendTranslatable("clan.ally.error.is_enemy")
            return
        }
        
        // Cria o convite
        AllyInviteService.createInvite(clanDao, clan, targetClan)
        
        sender.sendTranslatable(
            "clan.ally.invite.response",
            Argument.string("clan_name", targetClan.name)
        )
    }
    
    /**
     * Remove uma aliança.
     * /clan ally remove <clanName>
     */
    fun remove(sender: Player, clan: ClanEntity, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val targetClan = clanCache.getClanByName(args[0])
        if (targetClan == null) {
            sender.sendTranslatable("error.no_clan")
            return
        }
        
        // Verifica se são aliados
        if (!clanRelationDao.isRelation(clan.id!!, targetClan.id!!, Relation.ALLY)) {
            sender.sendTranslatable("clan.ally.list.empty")
            return
        }
        
        // Remove a relação
        clanRelationDao.removeRelation(clan.id, targetClan.id)
        
        // Notifica o outro clã
        targetClan.getLeader()?.sendTranslatable(
            "clan.ally.remove.target_response",
            Argument.string("clan_name", clan.name)
        )
        
        sender.sendTranslatable(
            "clan.ally.remove.response",
            Argument.string("clan_name", targetClan.name)
        )
    }
    
    /**
     * Aceita um convite de aliança.
     * /clan ally accept <clanName>
     */
    fun accept(sender: Player, clan: ClanEntity, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val senderClan = clanCache.getClanByName(args[0])
        if (senderClan == null) {
            sender.sendTranslatable("error.no_clan")
            return
        }
        
        // Verifica se existe convite
        val hasInvite = allyInviteDao.hasInvite(senderClan.id!!, clan.id!!)
        if (!hasInvite) {
            sender.sendTranslatable("clan.ally.error.no_invite")
            return
        }
        
        // Cria a relação de aliança (apenas uma entrada, com IDs ordenados)
        // O constraint do banco exige clan1_id < clan2_id
        val (smallerId, largerId) = if (clan.id < senderClan.id) {
            clan.id to senderClan.id
        } else {
            senderClan.id to clan.id
        }
        
        ClanRelationEntity(
            id = null,
            clan1Id = smallerId,
            clan2Id = largerId,
            relation = Relation.ALLY,
            active = true
        ).save<ClanRelationEntity>()
        
        // Remove o convite
        allyInviteDao.deleteInvite(senderClan.id, clan.id)
        allyInviteCache.invalidate(clan.id)
        
        // Notifica o clã que enviou o convite
        senderClan.getLeader()?.sendTranslatable(
            "clan.ally.accept.sender_response",
            Argument.string("clan_name", clan.name)
        )
        
        sender.sendTranslatable(
            "clan.ally.accept.response",
            Argument.string("clan_name", senderClan.name)
        )
    }
    
    /**
     * Rejeita um convite de aliança.
     * /clan ally reject <clanName>
     */
    fun reject(sender: Player, clan: ClanEntity, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }
        
        val senderClan = clanCache.getClanByName(args[0])
        if (senderClan == null) {
            sender.sendTranslatable("error.no_clan")
            return
        }
        
        // Verifica se existe convite
        val hasInvite = allyInviteDao.hasInvite(senderClan.id!!, clan.id!!)
        if (!hasInvite) {
            sender.sendTranslatable("clan.ally.error.no_invite")
            return
        }
        
        // Remove o convite
        allyInviteDao.deleteInvite(senderClan.id, clan.id)
        allyInviteCache.invalidate(clan.id)
        
        // Notifica o clã que enviou o convite
        senderClan.getLeader()?.sendTranslatable(
            "clan.ally.reject.sender_response",
            Argument.string("clan_name", clan.name)
        )
        
        sender.sendTranslatable(
            "clan.ally.reject.response",
            Argument.string("clan_name", senderClan.name)
        )
    }
    
    /**
     * Lista aliados do clã.
     * /clan ally list
     */
    fun list(sender: Player, clan: ClanEntity) {
        val allies = clanRelationDao.findRelatedClanNames(clan.id!!, Relation.ALLY)
        
        if (allies.isEmpty()) {
            sender.sendTranslatable("clan.ally.list.empty")
            return
        }
        
        sender.sendTranslatable(
            "clan.ally.list.header",
            Argument.numeric("count", allies.size)
        )
        
        allies.forEach { allyName ->
            sender.sendTranslatable(
                "clan.ally.list.entry",
                Argument.string("clan_name", allyName)
            )
        }
    }
    
    /**
     * Retorna sugestões de autocomplete para o subcomando.
     */
    fun getSuggestions(subCommand: String, clan: ClanEntity): List<String> {
        return when (subCommand.lowercase()) {
            AllySubCommands.ADD.command -> clanCache.getClanNames().filter { it != clan.name }
            AllySubCommands.REMOVE.command -> clanRelationDao.findRelatedClanNames(clan.id!!, Relation.ALLY)
            AllySubCommands.ACCEPT.command, AllySubCommands.REJECT.command -> allyInviteCache.getSenderClanNames(clan.id!!)
            else -> emptyList()
        }
    }
}
