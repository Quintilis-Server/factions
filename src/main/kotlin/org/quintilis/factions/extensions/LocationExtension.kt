package org.quintilis.factions.extensions

import net.kyori.adventure.text.Component
import org.bukkit.Location

fun Location.broadcastInRadius(radius: Double, message: Component) {
    this.world?.getNearbyPlayers(this, radius)?.forEach { player ->
        player.sendMessage(message)
    }
}