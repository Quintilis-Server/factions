package org.quintilis.factions.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.services.Services

/**
 * Extension functions para Player
 * Simplifica operações comuns relacionadas a clãs e mensagens
 */

/**
 * Envia uma mensagem traduzível para o jogador.
 * 
 * Uso: sender.sendTranslatable("clan.create.response", Argument.string("clan_name", name))
 */
fun Player.sendTranslatable(key: String, vararg args: ComponentLike) {
    this.sendMessage {
        Component.translatable(key, *args)
    }
}

/**
 * Envia uma mensagem traduzível simples (sem argumentos).
 * 
 * Uso: sender.sendTranslatable("error.no_clan")
 */
fun Player.sendTranslatable(key: String) {
    this.sendMessage {
        Component.translatable(key)
    }
}

/**
 * Obtém o clã do jogador (como membro), se existir.
 */
fun Player.getClan(): ClanEntity? {
    return Services.clanCache.getClanByMember(this.uniqueId)
}

/**
 * Obtém o clã do jogador como líder, se for líder de algum.
 */
fun Player.getClanAsLeader(): ClanEntity? {
    return Services.clanCache.getClanByLeaderId(this.uniqueId)
}

/**
 * Verifica se o jogador é líder de algum clã.
 */
fun Player.isClanLeader(): Boolean {
    return getClanAsLeader() != null
}

/**
 * Verifica se o jogador é membro de algum clã.
 */
fun Player.isInClan(): Boolean {
    return Services.clanCache.isMember(this.uniqueId)
}
