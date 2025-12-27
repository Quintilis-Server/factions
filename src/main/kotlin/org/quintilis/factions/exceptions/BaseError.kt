package org.quintilis.factions.exceptions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.tag.Tag
import java.lang.RuntimeException

open class BaseError: RuntimeException {

    val component: Component

    /**
     * Construtor Principal para Tradução.
     * @param logMessage Mensagem técnica para o console (ex: "Failed to load clan X").
     * @param key A chave de tradução no seu arquivo de lang (ex: "error.clan.not_found").
     * @param args Argumentos para substituir na mensagem (ex: nome do jogador, id).
     */
    constructor(logMessage: String, key: String, vararg args: ComponentLike): super(logMessage){
        this.component = Component.translatable(key, *args)
    }

    /**
     * Construtor simplificado (Console = Key).
     * Usa a própria chave como mensagem de log.
     */
    constructor(key: String, vararg args: ComponentLike) : super(key) {
        this.component = Component.translatable(key, *args)
    }

    /**
     * Construtor para componente direto (sem chave de tradução).
     * Útil se você quiser mandar uma mensagem hardcoded ou MiniMessage já parseado.
     */
    constructor(logMessage: String, directComponent: Component) : super(logMessage) {
        this.component = directComponent
    }
}