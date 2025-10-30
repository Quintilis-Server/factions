package org.quintilis.factions.listeners

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.quintilis.factions.entities.managers.ClaimManager
import org.quintilis.factions.entities.managers.ClanManager

class BlockProtectionListener : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val chunk = event.block.chunk
        val claim = ClaimManager.getClaim(chunk)

        if (claim != null) {

            val playerClan = ClanManager.getClanByPlayer(player)

            if (playerClan == null || playerClan.id != claim.clanId) {

            event.isCancelled = true
            player.sendMessage(Component.text("You cant modify blocks in chunk of this clan!").color(NamedTextColor.RED))

            }
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val chunk = event.block.chunk
        val claim = ClaimManager.getClaim(chunk)

        if (claim != null) {

            val playerClan = ClanManager.getClanByPlayer(player)

            if (playerClan == null || playerClan.id != claim.clanId) {

                event.isCancelled = true
                player.sendMessage(Component.text("You cant modify blocks in chunk of this clan!").color(NamedTextColor.RED))

            }
        }
    }
}