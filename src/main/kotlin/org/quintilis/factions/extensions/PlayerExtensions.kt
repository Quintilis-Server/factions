package org.quintilis.factions.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.results.Result
import org.quintilis.factions.services.FactionsServices
import org.quintilis.factions.services.FactionsServices.playerCache

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
 * Mostra um Title traduzível na tela do jogador.
 * 
 * Uso: sender.sendTitleTranslatable("title.key", "subtitle.key")
 */
fun Player.sendTitleTranslatable(
    titleKey: String,
    subtitleKey: String? = null,
    times: Title.Times? = null,
    vararg args: ComponentLike
) {
    val titleComponent = Component.translatable(titleKey, *args)
    val subtitleComponent = subtitleKey?.let { Component.translatable(it, *args) } ?: Component.empty()
    
    if (times != null) {
        this.showTitle(Title.title(titleComponent, subtitleComponent, times))
    } else {
        this.showTitle(Title.title(titleComponent, subtitleComponent))
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

fun Player.sendTranslatable(component: Component){
    this.sendMessage(component)
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

fun Player.getPlayerEntity(): PlayerEntity? {
    return playerCache.getPlayer(this.uniqueId)
}
