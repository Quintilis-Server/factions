package org.quintilis.factions.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.results.Result
import org.quintilis.factions.services.FactionsServices

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
 * Envia uma mensagem traduzível de um Result
 *
 * Uso: sender.sendTranslatable(result)
 */
fun Player.sendTranslatable(result: Result) {
    when (result) {
        is Result.Success -> {
            result.messageKey?.let { key ->
                // Chama sua função base que aceita (String, Map)
                this.sendTranslatable(key, result.args as ComponentLike)
            }
        }
        is Result.Error -> {
            this.sendTranslatable(result.messageKey, result.args as ComponentLike)
        }
    }
}

/**
 * Obtém o clã do jogador (como membro), se existir.
 */
fun Player.getClan(): ClanEntity? {
    return FactionsServices.clanCache.getClanByMember(this.uniqueId)
}

/**
 * Obtém o clã do jogador como líder, se for líder de algum.
 */
fun Player.getClanAsLeader(): ClanEntity? {
    return FactionsServices.clanCache.getClanByLeaderId(this.uniqueId)
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
    return FactionsServices.clanCache.isMember(this.uniqueId)
}
