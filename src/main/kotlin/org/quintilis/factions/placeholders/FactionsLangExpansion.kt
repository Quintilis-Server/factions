package org.quintilis.factions.placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.quintilis.factions.managers.TranslationManager

class FactionsLangExpansion(
    private val identifier: String,
    private val author: String,
    private val version: String,
): PlaceholderExpansion() {
    override fun getIdentifier(): String = identifier
    override fun getAuthor(): String = author
    override fun getVersion(): String = version
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if(player == null) return ""

        if (params.startsWith("lang_")) {
            val translationKey = params.removePrefix("lang_")

            return TranslationManager.getRawMessage(translationKey, player.locale())
        }

        return null
    }
}