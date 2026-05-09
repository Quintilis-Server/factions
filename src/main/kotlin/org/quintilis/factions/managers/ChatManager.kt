package org.quintilis.factions.managers

import org.bukkit.entity.Player
import java.util.UUID

object ChatManager{
    private val clanChatToggled = mutableSetOf<UUID>()

    fun toggleClanChat(player: Player): Boolean {
        val uuid = player.uniqueId
        return if (clanChatToggled.contains(uuid)) {
            clanChatToggled.remove(uuid)
            false // Desativou
        } else {
            clanChatToggled.add(uuid)
            true  // Ativou
        }
    }

    fun isClanChatToggled(player: Player) = clanChatToggled.contains(player.uniqueId)
}