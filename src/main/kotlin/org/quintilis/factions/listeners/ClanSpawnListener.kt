package org.quintilis.factions.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.services.FactionsServices.coreCache

@AutoRegister
class ClanSpawnListener: Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onRespawn(event: PlayerRespawnEvent){
        val player = event.player
        val clan = player.getClan() ?: return

        val deathLocation = player.location
        val nearestCore = coreCache.findClosestCoreByClan(clan.id!!, deathLocation)

        nearestCore?.getLocation()?.let { loc ->
            // Adiciona um pequeno offset no Y para não bugar no bloco
            event.respawnLocation = loc.clone().add(0.5, 1.1, 0.5)
            player.sendTranslatable("spawn.at_core")
        }
    }
}