package org.quintilis.factions.extensions

import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.title.Title
import org.quintilis.factions.entities.clan.ClanEntity

/**
 * Envia uma mensagem traduzível por texto para todos os membros online do clã.
 */
fun ClanEntity.broadcastTranslatable(key: String, vararg args: ComponentLike) {
    this.getMembers().mapNotNull { it.getPlayer() }.forEach {
        it.sendTranslatable(key, *args)
    }
}

/**
 * Envia um Title na tela de todos os membros online do clã.
 */
fun ClanEntity.broadcastTitleTranslatable(
    titleKey: String,
    subtitleKey: String? = null,
    times: Title.Times? = null,
    vararg args: ComponentLike
) {
    this.getMembers().mapNotNull { it.getPlayer() }.forEach {
        it.sendTitleTranslatable(titleKey, subtitleKey, times, *args)
    }
}
