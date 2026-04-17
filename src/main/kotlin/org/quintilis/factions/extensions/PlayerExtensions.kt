package org.quintilis.factions.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.translation.Argument
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
    val (key, argsMap) = when (result) {
        is Result.Success -> {
            val k = result.messageKey ?: return // Se não tem mensagem, não faz nada
            k to result.args
        }
        is Result.Error -> {
            result.messageKey to result.args
        }
    }

    // 2. Convertemos o Map em um Array de Argumentos (que são ComponentLike)
    // O JDBI/Postgres pode retornar valores como Double ou Int, então usamos .toString()
    val componentArgs = argsMap.map { (k, v) ->
        if (v is ComponentLike) {
            Argument.component(k, v)
        } else {
            Argument.string(k, v.toString())
        }
    }.toTypedArray()

    // 2. Usamos o operador spread (*) para passar o array para o vararg
    this.sendTranslatable(key, *componentArgs)
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
    return FactionsServices.clanMemberCache.isAnyMember(this.uniqueId)
}

fun Player.getPlayerEntity(): PlayerEntity? {
    return playerCache.getPlayer(this.uniqueId)
}
