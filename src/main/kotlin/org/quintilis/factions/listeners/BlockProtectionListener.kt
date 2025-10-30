package org.quintilis.factions.listeners

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.quintilis.factions.entities.managers.ClaimManager

class BlockProtectionListener : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val chunk = event.block.chunk
        val claim = ClaimManager.getClaim(chunk)

        if (claim != null && !player.hasPermission("factions.bypass")) {
            event.isCancelled = true
            player.sendMessage(Component.text("You cant break blocks in chunk of this clan!").color(NamedTextColor.RED))
        }
    }
}