package org.quintilis.factions.handlers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.services.AllyInviteService
import org.quintilis.factions.services.Services

/**
 * Handler para comandos de aliança (/clan ally).
 */
class AllyCommandHandler {
    
    private val clanCache get() = Services.clanCache
    private val clanRelationDao get() = Services.clanRelationDao
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
        AllyInviteService.createInvite(Services.clanDao, clan, targetClan)
        
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
        
        // TODO: Implementar remoção de aliança
        sender.sendTranslatable("error.not_implemented")
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
        
        // TODO: Implementar aceitação de aliança
        sender.sendTranslatable("error.not_implemented")
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
        
        // TODO: Implementar rejeição de aliança
        sender.sendTranslatable("error.not_implemented")
    }
    
    /**
     * Lista aliados do clã.
     * /clan ally list
     */
    fun list(sender: Player, clan: ClanEntity) {
        // TODO: Implementar listagem de aliados
        sender.sendTranslatable("error.not_implemented")
    }
    
    /**
     * Retorna sugestões de autocomplete para o subcomando.
     */
    fun getSuggestions(subCommand: String, clan: ClanEntity): List<String> {
        return when (subCommand.lowercase()) {
            "add" -> clanCache.getClanNames().filter { it != clan.name }
            "accept", "reject" -> allyInviteCache.getSenderClanNames(clan.id!!)
            else -> emptyList()
        }
    }
}
